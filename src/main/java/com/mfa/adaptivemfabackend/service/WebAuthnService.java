package com.mfa.adaptivemfabackend.service;

import com.mfa.adaptivemfabackend.dto.PasskeyOptionsResponse;
import com.mfa.adaptivemfabackend.dto.PasskeyVerifyRequest;
import com.mfa.adaptivemfabackend.entity.PasskeyCredential;
import com.mfa.adaptivemfabackend.entity.User;
import com.mfa.adaptivemfabackend.repository.PasskeyCredentialRepository;
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
    private PasskeyCredentialRepository passkeyCredentialRepository;

    @Autowired
    private UserRepository userRepository;

    private final Map<String, Challenge> challengeStore = new ConcurrentHashMap<>();

    @Value("${webauthn.relying-party.id:localhost}")
    private String rpId;

    @Value("${webauthn.origin:http://localhost:8080}")
    private String originUrl;

    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

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

    public void registerPasskey(String username, PasskeyVerifyRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

        Challenge challenge = challengeStore.get(username);
        if (challenge == null) {
            throw new RuntimeException("Challenge đã hết hạn hoặc không tồn tại!");
        }

        PasskeyCredential passkey = PasskeyCredential.builder()
                .credentialId(request.getCredentialId())
                .user(user)
                .publicKey(request.getAttestationObject() != null ? request.getAttestationObject() : "MOCK_PUBLIC_KEY")
                .signCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        passkeyCredentialRepository.save(passkey);
        challengeStore.remove(username);
    }

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

    public boolean verifyPasskey(String username, PasskeyVerifyRequest request) {
        Challenge challenge = challengeStore.get(username);
        if (challenge == null) {
            throw new RuntimeException("Challenge đã hết hạn hoặc không tồn tại!");
        }

        Optional<PasskeyCredential> passkeyOpt = passkeyCredentialRepository.findById(request.getCredentialId());
        if (passkeyOpt.isEmpty()) {
            return false;
        }

        PasskeyCredential passkey = passkeyOpt.get();
        passkey.setSignCount(passkey.getSignCount() + 1);
        passkeyCredentialRepository.save(passkey);

        challengeStore.remove(username);
        return true;
    }

    public boolean userHasPasskey(Integer userId) {
        return !passkeyCredentialRepository.findByUser_UserId(userId).isEmpty();
    }
}