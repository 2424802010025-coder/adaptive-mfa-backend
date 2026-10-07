package com.mfa.adaptivemfabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollDeviceRequest {
    private String username;
    private String deviceName;
    private String browserOs; // Bổ sung trường này để tránh lỗi NOT NULL trong CSDL
}