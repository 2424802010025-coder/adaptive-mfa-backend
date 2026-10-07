package com.mfa.adaptivemfabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String username;
    private String password;
    private Boolean rememberDevice; // Bổ sung trường này cho Giải pháp 2 & 3
}