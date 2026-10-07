package com.mfa.adaptivemfabackend.repository;

import com.mfa.adaptivemfabackend.entity.PendingMfaRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingMfaRequestRepository extends JpaRepository<PendingMfaRequest, String> {

    Optional<PendingMfaRequest> findByRequestIdAndStatus(String requestId, String status);

    // Bổ sung: Tìm request PENDING mới nhất của một User (dùng cho Push Notification / App)
    Optional<PendingMfaRequest> findTopByUser_UserIdAndStatusOrderByCreatedAtDesc(Integer userId, String status);

    // Bổ sung: Tìm danh sách các request đã quá hạn để Cron Job xử lý
    List<PendingMfaRequest> findByStatusAndExpiresAtBefore(String status, LocalDateTime now);

    // Bổ sung: Lệnh UPDATE hàng loạt trực tiếp trong Database để tối ưu hiệu năng Cron Job
    @Transactional
    @Modifying
    @Query("UPDATE PendingMfaRequest p SET p.status = 'EXPIRED' WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    int expireOldPendingRequests(@Param("now") LocalDateTime now);
}