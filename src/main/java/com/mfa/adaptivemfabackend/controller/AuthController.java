package com.mfa.adaptivemfabackend.controller;

import com.mfa.adaptivemfabackend.dto.*;
import com.mfa.adaptivemfabackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 1. API Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String msg = authService.register(request);
        return ResponseEntity.ok(Map.of(
                "message", msg,
                "enrollmentId", request.getUsername()
        ));
    }

    /**
     * 2. API Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. API Phê duyệt / Từ chối xác thực MFA (Gọi từ App di động hoặc giả lập Postman)
     */
    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request) {
        String result = authService.verifyMfa(request);
        return ResponseEntity.ok(Map.of("message", result));
    }

    /**
     * 4. API Lắng nghe (Polling) trạng thái phê duyệt MFA của lượt Đăng nhập
     */
    @GetMapping("/mfa-status/{requestId}")
    public ResponseEntity<Map<String, Object>> checkMfaStatus(@PathVariable String requestId) {
        return ResponseEntity.ok(authService.checkMfaStatus(requestId));
    }

    /**
     * 5. API Lắng nghe (Polling) trạng thái Kích hoạt Vân tay lần đầu khi Đăng ký
     */
    @GetMapping("/enroll-status/{enrollmentId}")
    public ResponseEntity<Map<String, Object>> checkEnrollStatus(@PathVariable String enrollmentId) {
        return ResponseEntity.ok(authService.checkEnrollStatus(enrollmentId));
    }

    /**
     * 6. API Kích hoạt & Liên kết Sinh trắc học (Sử dụng EnrollDeviceRequest DTO)
     */
    @PostMapping("/enroll-device")
    public ResponseEntity<?> enrollDevice(@RequestBody EnrollDeviceRequest request) {
        // ĐÃ SỬA: Truyền đủ 3 tham số (username, deviceName, browserOs)
        authService.enrollDevice(request.getUsername(), request.getDeviceName(), request.getBrowserOs());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Thiết bị di động đã được liên kết thành công!"
        ));
    }
}