package com.mfa.adaptivemfabackend.controller;

import com.mfa.adaptivemfabackend.dto.PasskeyOptionsResponse;
import com.mfa.adaptivemfabackend.dto.PasskeyVerifyRequest;
import com.mfa.adaptivemfabackend.service.WebAuthnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webauthn")
@CrossOrigin(origins = "*")
public class WebAuthnController {

    @Autowired
    private WebAuthnService webAuthnService;

    // 1. Lấy Challenge & Options để đăng ký Passkey mới
    @GetMapping("/register/options")
    public ResponseEntity<PasskeyOptionsResponse> getRegisterOptions(@RequestParam String username) {
        return ResponseEntity.ok(webAuthnService.generateRegistrationOptions(username));
    }

    // 2. Nhận kết quả từ Android Client để hoàn thành đăng ký Passkey
    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyRegistration(
            @RequestParam String username,
            @RequestBody PasskeyVerifyRequest request) {
        try {
            webAuthnService.registerPasskey(username, request);
            return ResponseEntity.ok(Map.of("message", "Đăng ký Passkey thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Lấy Challenge & Options cho xác thực Adaptive MFA (khi Trust Score < 50)
    @GetMapping("/authenticate/options")
    public ResponseEntity<PasskeyOptionsResponse> getAuthenticateOptions(@RequestParam String username) {
        return ResponseEntity.ok(webAuthnService.generateAssertionOptions(username));
    }

    // 4. Xác thực chữ ký Passkey từ Android Client
    @PostMapping("/authenticate/verify")
    public ResponseEntity<?> verifyAuthentication(
            @RequestParam String username,
            @RequestBody PasskeyVerifyRequest request) {
        try {
            boolean isValid = webAuthnService.verifyPasskey(username, request);
            if (isValid) {
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Xác thực Passkey thành công!"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Xác thực Passkey thất bại!"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 5. Kiểm tra xem user đã có Passkey chưa
    @GetMapping("/check/{userId}")
    public ResponseEntity<?> checkUserPasskey(@PathVariable Integer userId) {
        boolean hasPasskey = webAuthnService.userHasPasskey(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "hasPasskey", hasPasskey));
    }
}