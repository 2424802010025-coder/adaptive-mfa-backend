package com.mfa.adaptivemfabackend.service;

import com.mfa.adaptivemfabackend.config.JwtTokenProvider;
import com.mfa.adaptivemfabackend.dto.*;
import com.mfa.adaptivemfabackend.entity.*;
import com.mfa.adaptivemfabackend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final SecurityLogRepository logRepository;
    private final PendingMfaRequestRepository pendingMfaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAgentParserService userAgentParser;
    private final TrustScoreService trustScoreService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        return "Đăng ký tài khoản thành công!";
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!"));

        // 1. Thu thập thông tin Ngữ cảnh IP & Thiết bị
        String userAgentStr = httpRequest.getHeader("User-Agent");
        String deviceName = userAgentParser.parseDeviceName(userAgentStr);
        String browserOs = userAgentParser.parseBrowserOs(userAgentStr);
        String clientIp = getClientIp(httpRequest);

        // 2. Kiểm tra Mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            saveLog(user, deviceName, clientIp, 0, "LAYER_1_PASSWORD", "FAILED");
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        // 3. Tính điểm Trust Score
        int trustScore = trustScoreService.calculateTrustScore(user, clientIp, deviceName);
        String authLayer = trustScoreService.determineAuthLayer(trustScore);

        // 4. Lưu / Cập nhật thông tin thiết bị
        String deviceId = "DEV_" + user.getUserId() + "_" + Math.abs(deviceName.hashCode());
        Device device = deviceRepository.findById(deviceId).orElseGet(() ->
                Device.builder()
                        .deviceId(deviceId)
                        .user(user)
                        .isTrusted(false)
                        .build()
        );
        device.setDeviceName(deviceName);
        device.setBrowserOs(browserOs != null ? browserOs : "Unknown OS");
        device.setLastIp(clientIp);
        device.setLastActive(LocalDateTime.now());

        boolean isRememberRequested = Boolean.TRUE.equals(request.getRememberDevice());
        if (isRememberRequested && trustScore >= 80) {
            device.setIsTrusted(true);
        }
        deviceRepository.save(device);

        boolean isTrustedDevice = Boolean.TRUE.equals(device.getIsTrusted());

        String mfaRequestId = null;
        String mfaCode = null;
        String finalJwtToken = null;

        // 5. Xử lý phân luồng Rủi ro (Risk-Based Authorization)
        if (trustScore < 80) {
            // RỦI RO TRUNG BÌNH / CAO: Yêu cầu xác thực MFA qua App di động
            // CHÚ Ý: TOKEN PHẢI LÀ NULL ĐỂ TRÁNH VÔ HIỆU HÓA MFA (MFA BYPASS FIX)
            mfaRequestId = UUID.randomUUID().toString();
            mfaCode = String.valueOf(ThreadLocalRandom.current().nextInt(10, 100));

            PendingMfaRequest mfaRequest = PendingMfaRequest.builder()
                    .requestId(mfaRequestId)
                    .user(user)
                    .fromDeviceName(deviceName)
                    .fromIp(clientIp)
                    .status("PENDING")
                    .mfaCode(mfaCode)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();
            pendingMfaRepository.save(mfaRequest);

            // Gửi dữ liệu qua WebSocket sang App Android
            Map<String, Object> pushPayload = new HashMap<>();
            pushPayload.put("requestId", mfaRequestId);
            pushPayload.put("username", user.getUsername());
            pushPayload.put("deviceName", deviceName);
            pushPayload.put("ipAddress", clientIp);
            pushPayload.put("trustScore", trustScore);
            pushPayload.put("mfaCode", mfaCode);
            pushPayload.put("message", "Yêu cầu xác thực đăng nhập từ thiết bị: " + deviceName);
            pushPayload.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/mfa-requests", (Object) pushPayload);
        } else {
            // RỦI RO THẤP (Trust Score >= 80): Cho phép đăng nhập thẳng và cấp Access Token
            finalJwtToken = jwtTokenProvider.generateToken(user.getUsername(), isTrustedDevice);
        }

        // 6. Ghi nhật ký Security Log
        saveLog(user, deviceName, clientIp, trustScore, authLayer, "SUCCESS");

        return LoginResponse.builder()
                .success(true)
                .message("Đánh giá điểm rủi ro hoàn tất")
                .trustScore(trustScore)
                .authLayer(authLayer)
                .requestId(mfaRequestId)
                .mfaCode(mfaCode)
                .token(finalJwtToken) // Sẽ chỉ có giá trị khi trustScore >= 80
                .build();
    }

    @Transactional
    public String verifyMfa(MfaVerifyRequest request) {
        PendingMfaRequest mfaRequest = pendingMfaRepository.findById(request.getRequestId())
                .orElseThrow(() -> new RuntimeException("Yêu cầu MFA không tồn tại hoặc đã hết hạn!"));

        // Kiểm tra xem Yêu cầu MFA đã xử lý trước đó chưa
        if (!"PENDING".equalsIgnoreCase(mfaRequest.getStatus())) {
            return "Yêu cầu xác thực này đã xử lý hoặc không còn hiệu lực!";
        }

        // Kiểm tra Hết hạn thời gian (5 phút)
        if (mfaRequest.getExpiresAt() != null && mfaRequest.getExpiresAt().isBefore(LocalDateTime.now())) {
            mfaRequest.setStatus("EXPIRED");
            pendingMfaRepository.save(mfaRequest);
            return "Yêu cầu xác thực MFA đã quá hạn!";
        }

        // Nếu Approve, kiểm tra Khớp mã Number Matching (2 chữ số)
        if ("APPROVED".equalsIgnoreCase(request.getStatus())) {
            if (request.getMfaCode() == null || !request.getMfaCode().trim().equals(mfaRequest.getMfaCode())) {
                mfaRequest.setStatus("REJECTED");
                pendingMfaRepository.save(mfaRequest);
                return "Mã xác thực không trùng khớp! Đã từ chối đăng nhập.";
            }
        }

        mfaRequest.setStatus(request.getStatus());
        pendingMfaRepository.save(mfaRequest);

        if ("APPROVED".equalsIgnoreCase(request.getStatus())) {
            return "Xác thực MFA thành công! Đăng nhập hoàn tất.";
        } else {
            return "Đã từ chối đăng nhập!";
        }
    }

    @Transactional
    public Map<String, Object> checkMfaStatus(String requestId) {
        Map<String, Object> response = new HashMap<>();

        PendingMfaRequest mfaRequest = pendingMfaRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy requestId: " + requestId));

        // FIX LAZY CHECK TIMEOUT: Tự động chuyển sang EXPIRED nếu quá 5 phút chưa xử lý
        if ("PENDING".equalsIgnoreCase(mfaRequest.getStatus())
                && mfaRequest.getExpiresAt() != null
                && mfaRequest.getExpiresAt().isBefore(LocalDateTime.now())) {
            mfaRequest.setStatus("EXPIRED");
            pendingMfaRepository.save(mfaRequest);
        }

        String status = mfaRequest.getStatus();
        response.put("status", status);

        if ("APPROVED".equalsIgnoreCase(status)) {
            User user = mfaRequest.getUser();
            String deviceId = "DEV_" + user.getUserId() + "_" + Math.abs(mfaRequest.getFromDeviceName().hashCode());
            Device device = deviceRepository.findById(deviceId).orElse(null);

            boolean isTrusted = (device != null) && Boolean.TRUE.equals(device.getIsTrusted());
            String finalAccessToken = jwtTokenProvider.generateToken(user.getUsername(), isTrusted);

            response.put("success", true);
            response.put("message", "Xác thực thành công!");
            response.put("accessToken", finalAccessToken);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            response.put("success", false);
            response.put("message", "Yêu cầu xác thực đã bị từ chối hoặc mã khớp số không đúng.");
        } else if ("EXPIRED".equalsIgnoreCase(status)) {
            response.put("success", false);
            response.put("message", "Yêu cầu xác thực MFA đã hết hạn.");
        } else {
            response.put("success", false);
            response.put("message", "Đang chờ xác thực trên ứng dụng di động...");
        }

        return response;
    }

    public Map<String, Object> checkEnrollStatus(String username) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            response.put("status", "NOT_FOUND");
            response.put("message", "Tài khoản không tồn tại!");
            return response;
        }

        // Tối ưu hiệu năng: Truy vấn trực tiếp các thiết bị Trusted thay vì dùng findAll()
        boolean isEnrolled = !deviceRepository.findByUser_UserIdAndIsTrustedTrue(user.getUserId()).isEmpty();

        if (isEnrolled) {
            response.put("status", "COMPLETED");
            response.put("message", "Liên kết sinh trắc học thành công!");
        } else {
            response.put("status", "PENDING");
            response.put("message", "Đang chờ liên kết sinh trắc học từ App di động...");
        }

        return response;
    }

    @Transactional
    public void enrollDevice(String username, String deviceName, String browserOs) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

        String deviceId = "DEV_MOBILE_" + user.getUserId();
        Device device = deviceRepository.findById(deviceId).orElseGet(() ->
                Device.builder()
                        .deviceId(deviceId)
                        .user(user)
                        .build()
        );

        device.setDeviceName(deviceName);
        device.setBrowserOs(browserOs != null ? browserOs : "Android Mobile App"); // Đảm bảo không bị NULL
        device.setIsTrusted(true);
        device.setLastActive(LocalDateTime.now());

        deviceRepository.save(device);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Nếu IP đi qua nhiều proxy: "Client_IP, Proxy1_IP, Proxy2_IP" -> Lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private void saveLog(User user, String deviceName, String ip, int trustScore, String layer, String status) {
        SecurityLog log = SecurityLog.builder()
                .user(user)
                .username(user != null ? user.getUsername() : "UNKNOWN")
                .deviceName(deviceName)
                .ipAddress(ip)
                .trustScore(trustScore)
                .authLayerUsed(layer)
                .status(status)
                .build();
        logRepository.save(log);
    }
}