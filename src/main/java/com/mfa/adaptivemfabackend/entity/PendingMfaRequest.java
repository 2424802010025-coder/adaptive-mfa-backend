package com.mfa.adaptivemfabackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_mfa_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingMfaRequest {

    @Id
    @Column(name = "request_id", length = 100)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "from_device_name", nullable = false, length = 100)
    @Builder.Default
    private String fromDeviceName = "Unknown Device";

    @Column(name = "from_ip", nullable = false, length = 45)
    @Builder.Default
    private String fromIp = "127.0.0.1";

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, EXPIRED

    @Column(name = "mfa_code", length = 10)
    private String mfaCode;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.fromDeviceName == null || this.fromDeviceName.isBlank()) this.fromDeviceName = "Unknown Device";
        if (this.fromIp == null || this.fromIp.isBlank()) this.fromIp = "127.0.0.1";
        if (this.expiresAt == null) this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }
}