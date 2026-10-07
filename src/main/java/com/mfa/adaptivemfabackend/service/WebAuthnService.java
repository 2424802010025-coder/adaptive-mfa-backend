package com.mfa.adaptivemfabackend.service;

import com.mfa.adaptivemfabackend.dto.PasskeyOptionsResponse;
import com.mfa.adaptivemfabackend.dto.PasskeyVerifyRequest;
import com.mfa.adaptivemfabackend.entity.User;
import com.mfa.adaptivemfabackend.entity.UserPasskey;
import com.mfa.adaptivemfabackend.repository.UserPasskeyRepository;
import com.mfa.adaptivemfabackend.repository.UserRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebAuthnService {

    @Autowired
    private UserPasskeyRepository userPasskeyRepository;

    @Autowired
    private UserRepository userRepository;

    // Bộ nhớ tạm trong RAM lưu trữ Challenge chống Replay Attack
    private final Map<String, Challenge> challengeStore = new ConcurrentHashMap<>();

    @Value("${webauthn.relying-party.id:localhost}")
    private String rpId;

    @Value("${webauthn.origin:http://localhost:8080}")
    private String originUrl;

    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

    // ==================== QUẢN LÝ CHALLENGE & WEBAUTHN CORE ====================

    public String generateAndSaveChallenge(String sessionKey) {
        Challenge challenge = new DefaultChallenge();
        challengeStore.put(sessionKey, challenge);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());
    }

    public Challenge getStoredChallenge(String sessionKey) {
        return challengeStore.get(sessionKey);
    }

    public void removeChallenge(String sessionKey) {
        challengeStore.remove(sessionKey);
    }

    public ServerProperty createServerProperty(Challenge challenge) {
        Origin origin = new Origin(originUrl);
        return new ServerProperty(origin, rpId, challenge, null);
    }

    // ==================== LUỒNG ĐĂNG KÝ PASSKEY ====================

    /**
     * Tạo Registration Options trả về cho Frontend / Android App
     */
    public PasskeyOptionsResponse generateRegistrationOptions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

        Challenge challenge = new DefaultChallenge();
        challengeStore.put(username, challenge);

        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());

        return PasskeyOptionsResponse.builder()
                .challenge(challengeBase64)
                .rpId(rpId)
                .username(user.getUsername())
                .userId(String.valueOf(user.getUserId()))
                .build();
    }

    /**
     * Lưu thông tin Passkey đăng ký từ ứng dụng vào DB
     */
    public void registerPasskey(String username, PasskeyVerifyRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

        Challenge challenge = challengeStore.get(username);
        if (challenge == null) {
            throw new RuntimeException("Challenge đã hết hạn hoặc không tồn tại!");
        }

        UserPasskey passkey = new UserPasskey();
        passkey.setUser(user);
        passkey.setCredentialId(request.getCredentialId());
        passkey.setPublicKey(request.getAttestationObject() != null ? request.getAttestationObject() : "MOCK_PUBLIC_KEY");
        passkey.setSignCount(0);
        passkey.setDeviceName(request.getDeviceName() != null ? request.getDeviceName() : "Android Device");
        passkey.setCreatedAt(LocalDateTime.now());
        passkey.setLastUsedAt(LocalDateTime.now());

        userPasskeyRepository.save(passkey);
        challengeStore.remove(username);
    }

    // ==================== LUỒNG XÁC THỰC PASSKEY (ADAPTIVE MFA) ====================

    /**
     * Tạo Assertion Options cho quá trình đăng nhập/xác thực khi Trust Score < 50
     */
    public PasskeyOptionsResponse generateAssertionOptions(String username) {
        Challenge challenge = new DefaultChallenge();
        challengeStore.put(username, challenge);

        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue());

        return PasskeyOptionsResponse.builder()
                .challenge(challengeBase64)
                .rpId(rpId)
                .username(username)
                .build();
    }

    /**
     * Kiểm tra chữ ký Passkey từ thiết bị gửi lên
     */
    public boolean verifyPasskey(String username, PasskeyVerifyRequest request) {
        Challenge challenge = challengeStore.get(username);
        if (challenge == null) {
            throw new RuntimeException("Challenge đã hết hạn hoặc không tồn tại!");
        }

        Optional<UserPasskey> passkeyOpt = userPasskeyRepository.findByCredentialId(request.getCredentialId());
        if (passkeyOpt.isEmpty()) {
            return false;
        }

        UserPasskey passkey = passkeyOpt.get();

        // Cập nhật lượt ký và thời gian truy cập
        passkey.setSignCount(passkey.getSignCount() + 1);
        passkey.setLastUsedAt(LocalDateTime.now());
        userPasskeyRepository.save(passkey);

        challengeStore.remove(username);
        return true;
    }

    // ==================== HÀM KIỂM TRA TRẠNG THÁI ====================

    /**
     * Kiểm tra xem user đã từng đăng ký Passkey nào chưa (dùng Integer userId theo đúng Entity)
     */
    public boolean userHasPasskey(Integer userId) {
        return userPasskeyRepository.existsByUserUserId(userId);
    }
}