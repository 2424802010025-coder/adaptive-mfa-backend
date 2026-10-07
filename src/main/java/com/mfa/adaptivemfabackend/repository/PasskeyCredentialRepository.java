package com.mfa.adaptivemfabackend.repository;

import com.mfa.adaptivemfabackend.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, String> {

    List<PasskeyCredential> findByUser_UserId(Integer userId);

    // Bổ sung: Tìm Passkey cụ thể thuộc về đúng User (tránh việc dùng credentialId của người khác)
    Optional<PasskeyCredential> findByCredentialIdAndUser_UserId(String credentialId, Integer userId);
}