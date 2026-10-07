package com.mfa.adaptivemfabackend.service;

import com.mfa.adaptivemfabackend.entity.Device;
import com.mfa.adaptivemfabackend.entity.User;
import com.mfa.adaptivemfabackend.repository.DeviceRepository;
import com.mfa.adaptivemfabackend.repository.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreService {

    private final DeviceRepository deviceRepository;
    private final SecurityLogRepository securityLogRepository;

    // Các trọng số điểm (Scoring Constants)
    private static final int BASE_SCORE = 50;
    private static final int BONUS_KNOWN_DEVICE = 15;
    private static final int BONUS_TRUSTED_DEVICE = 25;
    private static final int BONUS_SAME_IP = 10;
    private static final int PENALTY_NEW_DEVICE = -20;
    private static final int PENALTY_FAILED_ATTEMPT_UNIT = -10; // Trừ 10 điểm cho mỗi lần đăng nhập sai gần đây

    /**
     * Tính toán điểm tin cậy (Trust Score) từ 0 đến 100 dựa trên ngữ cảnh và lịch sử đăng nhập.
     */
    public int calculateTrustScore(User user, String currentIp, String currentDeviceName) {
        int score = BASE_SCORE;

        // 1. Kiểm tra tính hợp lệ của Device Name (Tránh bẫy "Unknown Device")
        boolean isGenericDevice = currentDeviceName == null
                || currentDeviceName.isBlank()
                || currentDeviceName.equalsIgnoreCase("Unknown Device")
                || currentDeviceName.equalsIgnoreCase("Unknown OS");

        if (!isGenericDevice) {
            Optional<Device> matchedDevice = deviceRepository.findByUser_UserIdAndDeviceName(
                    user.getUserId(), currentDeviceName
            );

            if (matchedDevice.isPresent()) {
                Device device = matchedDevice.get();
                score += BONUS_KNOWN_DEVICE; // Thiết bị quen thuộc đã từng đăng nhập

                if (Boolean.TRUE.equals(device.getIsTrusted())) {
                    score += BONUS_TRUSTED_DEVICE; // Thiết bị đã được đánh dấu Tin tưởng
                }

                // Kiểm tra trùng khớp IP (Bỏ qua nếu là IP Localhost/Nội bộ)
                if (currentIp != null && !isLocalhostIp(currentIp) && currentIp.equals(device.getLastIp())) {
                    score += BONUS_SAME_IP;
                }
            } else {
                score += PENALTY_NEW_DEVICE; // Thiết bị hoàn toàn mới
            }
        } else {
            // Nếu không nhận diện được tên thiết bị (VD: curl hoặc client không gửi Header)
            score += PENALTY_NEW_DEVICE;
        }

        // 2. Phát hiện hành vi bất thường (Phạt điểm nếu có nhiều lần đăng nhập thất bại gần đây)
        LocalDateTime window15Mins = LocalDateTime.now().minusMinutes(15);
        long recentFailedAttempts = securityLogRepository.countByUser_UserIdAndStatusAndTimestampAfter(
                user.getUserId(), "FAILED", window15Mins
        );

        if (recentFailedAttempts > 0) {
            int totalPenalty = (int) (recentFailedAttempts * PENALTY_FAILED_ATTEMPT_UNIT);
            score += totalPenalty; // Trừ điểm rủi ro do nghi vấn Brute-force
            log.warn("User {} có {} lần đăng nhập thất bại trong 15 phút qua. Trừ {} điểm.",
                    user.getUsername(), recentFailedAttempts, Math.abs(totalPenalty));
        }

        // 3. Khống chế điểm luôn nằm trong đoạn [0, 100]
        int finalScore = Math.max(0, Math.min(100, score));
        log.info("Tính toán Trust Score cho User [{}]: Device='{}', IP='{}' -> Score = {}",
                user.getUsername(), currentDeviceName, currentIp, finalScore);

        return finalScore;
    }

    /**
     * Quyết định tầng xác thực (Auth Layer) dựa trên Trust Score
     */
    public String determineAuthLayer(int trustScore) {
        if (trustScore >= 80) {
            return "LOW_RISK_DIRECT_LOGIN"; // Rủi ro thấp (Score 80-100) -> Cho phép đăng nhập ngay
        } else if (trustScore >= 50) {
            return "MEDIUM_RISK_MFA_PUSH";  // Rủi ro trung bình (Score 50-79) -> Yêu cầu xác thực MFA Push Notification
        } else {
            return "HIGH_RISK_STRICT_MFA";  // Rủi ro cao (Score 0-49) -> Bắt buộc xác thực nghiêm ngặt
        }
    }

    private boolean isLocalhostIp(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip);
    }
}