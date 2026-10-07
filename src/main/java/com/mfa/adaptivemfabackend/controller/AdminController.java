package com.mfa.adaptivemfabackend.controller;

import com.mfa.adaptivemfabackend.entity.Device;
import com.mfa.adaptivemfabackend.entity.SecurityLog;
import com.mfa.adaptivemfabackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 1. API Lấy 50 nhật ký đăng nhập & đánh giá rủi ro mới nhất
     */
    @GetMapping("/logs")
    public ResponseEntity<List<SecurityLog>> getLatestAuditLogs() {
        return ResponseEntity.ok(adminService.getLatestAuditLogs());
    }

    /**
     * 2. API Lấy danh sách tất cả các thiết bị trong hệ thống
     */
    @GetMapping("/devices")
    public ResponseEntity<List<Device>> getAllDevices() {
        return ResponseEntity.ok(adminService.getAllDevices());
    }

    /**
     * 3. API Thu hồi quyền tin cậy / Xóa thiết bị
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> revokeDevice(@PathVariable String deviceId) {
        adminService.revokeDevice(deviceId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã thu hồi quyền và xóa thiết bị khỏi hệ thống!"
        ));
    }
}