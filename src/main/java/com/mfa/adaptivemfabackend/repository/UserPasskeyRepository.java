package com.mfa.adaptivemfabackend.repository;

import com.mfa.adaptivemfabackend.entity.User;
import com.mfa.adaptivemfabackend.entity.UserPasskey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPasskeyRepository extends JpaRepository<UserPasskey, Long> {

    Optional<UserPasskey> findByCredentialId(String credentialId);

    List<UserPasskey> findByUser(User user);

    List<UserPasskey> findByUserUserId(Integer userId);

    boolean existsByUserUserId(Integer userId);
}