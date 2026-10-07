package com.mfa.adaptivemfabackend.service;

import com.mfa.adaptivemfabackend.repository.PendingMfaRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaCleanupScheduler {

    private final PendingMfaRequestRepository pendingMfaRepository;

    /**
     * Chạy định kỳ mỗi 10 phút để dọn dẹp các Yêu cầu MFA đã quá hạn 5 phút
     */
    @Scheduled(fixedRate = 600000) // 600,000 ms = 10 phút
    @Transactional
    public void cleanupExpiredMfaRequests() {
        LocalDateTime now = LocalDateTime.now();

        // Bạn có thể xóa hẳn hoặc cập nhật trạng thái EXPIRED
        pendingMfaRepository.findAll().stream()
                .filter(req -> "PENDING".equalsIgnoreCase(req.getStatus())
                        && req.getExpiresAt() != null
                        && req.getExpiresAt().isBefore(now))
                .forEach(req -> {
                    req.setStatus("EXPIRED");
                    pendingMfaRepository.save(req);
                });

        log.info("Đã quét và cập nhật trạng thái EXPIRED cho các yêu cầu MFA quá hạn lúc {}", now);
    }
}