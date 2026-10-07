package com.mfa.adaptivemfabackend.service;

import com.mfa.adaptivemfabackend.entity.Device;
import com.mfa.adaptivemfabackend.entity.SecurityLog;
import com.mfa.adaptivemfabackend.repository.DeviceRepository;
import com.mfa.adaptivemfabackend.repository.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final SecurityLogRepository securityLogRepository;
    private final DeviceRepository deviceRepository;

    public List<SecurityLog> getLatestAuditLogs() {
        return securityLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Transactional
    public void revokeDevice(String deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new RuntimeException("Không tìm thấy mã thiết bị!");
        }
        deviceRepository.deleteById(deviceId);
    }
}