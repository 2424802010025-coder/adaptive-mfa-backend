package com.mfa.adaptivemfabackend.repository;

import com.mfa.adaptivemfabackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    // Bổ sung: Tìm theo email (hữu ích cho tính năng quên mật khẩu hoặc xác thực qua Email)
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}