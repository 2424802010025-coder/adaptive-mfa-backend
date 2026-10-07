package com.mfa.adaptivemfabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyVerifyRequest {
    private String credentialId;
    private String attestationObject;
    private String clientDataJSON;
    private String deviceName;
}