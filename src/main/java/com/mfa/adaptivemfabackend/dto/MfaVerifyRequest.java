package com.mfa.adaptivemfabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {
    private String requestId;
    private String status;  // "APPROVED" hoặc "REJECTED"
    private String mfaCode; // Mã 2 chữ số người dùng nhập từ App Android để khớp với Web
}