package com.mfa.adaptivemfabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyOptionsResponse {
    private String challenge;
    private String rpId;
    private String username;
    private String userId;
}