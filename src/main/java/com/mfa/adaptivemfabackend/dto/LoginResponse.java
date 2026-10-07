package com.mfa.adaptivemfabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private Integer trustScore;
    private String authLayer;
    private String requestId; // Dùng cho trường hợp kích hoạt MFA kênh ngoài
    private String mfaCode;   // Mã 2 chữ số dùng cho cơ chế Number Matching (VD: "42")
    private String token;     // Chứa JWT Token thực thụ (30 phút hoặc 7 ngày)
}