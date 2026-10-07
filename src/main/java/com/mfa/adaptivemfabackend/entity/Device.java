package com.mfa.adaptivemfabackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @Column(name = "device_id")
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_name", nullable = false, length = 100)
    @Builder.Default
    private String deviceName = "Unknown Device";

    @Column(name = "browser_os", nullable = false, length = 100)
    @Builder.Default
    private String browserOs = "Unknown OS";

    @Column(name = "last_ip", nullable = false, length = 45)
    @Builder.Default
    private String lastIp = "127.0.0.1";

    @Column(name = "is_trusted")
    @Builder.Default
    private Boolean isTrusted = false;

    @Column(name = "last_active")
    private LocalDateTime lastActive;

    @PrePersist
    @PreUpdate
    public void ensureDefaults() {
        if (this.deviceName == null || this.deviceName.isBlank()) this.deviceName = "Unknown Device";
        if (this.browserOs == null || this.browserOs.isBlank()) this.browserOs = "Unknown OS";
        if (this.lastIp == null || this.lastIp.isBlank()) this.lastIp = "127.0.0.1";
        if (this.isTrusted == null) this.isTrusted = false;
        if (this.lastActive == null) this.lastActive = LocalDateTime.now();
    }
}