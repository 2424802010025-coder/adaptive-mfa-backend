package com.mfa.adaptivemfabackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_passkeys", indexes = {
        @Index(name = "idx_credential_id", columnList = "credentialId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPasskey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Credential ID dạng mã hóa Base64URL do Trình duyệt / Passkey tạo ra
    @Column(nullable = false, unique = true, length = 512)
    private String credentialId;

    // Public Key dạng COSE/PEM dùng để verify chữ ký số của các lần đăng nhập sau
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    // Anti-Replay Counter: Lưu số lần ký của thiết bị.
    // Nếu counter gửi lên <= signCount trong DB -> Cảnh báo Replay Attack
    @Column(nullable = false)
    private long signCount;

    // Tên thiết bị hiển thị (ví dụ: "Windows Hello", "MacBook TouchID", "YubiKey 5")
    @Column(length = 100)
    private String deviceName;

    // Authenticator Attestation GUID (Định danh nhà sản xuất thiết bị)
    @Column(length = 64)
    private String aaguid;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastUsedAt = LocalDateTime.now();
}