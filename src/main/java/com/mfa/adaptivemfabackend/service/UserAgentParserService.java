package com.mfa.adaptivemfabackend.service;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserAgentParserService {

    private final UserAgentAnalyzer uaa;

    public UserAgentParserService() {
        // Tối ưu YAUAA: Chỉ load 4 trường cần thiết thay vì toàn bộ bộ quy tắc YAUAA
        // Giúp tiết kiệm bộ nhớ RAM và tăng tốc độ boot ứng dụng Spring Boot
        this.uaa = UserAgentAnalyzer
                .newBuilder()
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR)
                .withField(UserAgent.DEVICE_CLASS)
                .hideMatcherLoadStats()
                .withCache(2000)
                .build();
    }

    /**
     * Phân tích chuỗi User-Agent ra tên thiết bị (Ví dụ: Chrome on Windows (Desktop))
     */
    public String parseDeviceName(String userAgentHeader) {
        if (userAgentHeader == null || userAgentHeader.isBlank()) {
            return "Unknown Device"; // Trả về Unknown để TrustScoreService phạt điểm rủi ro
        }

        try {
            UserAgent agent = uaa.parse(userAgentHeader);

            String agentName = agent.getValue(UserAgent.AGENT_NAME);
            String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
            String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);

            if (isUnknown(agentName)) agentName = "Unknown Browser";
            if (isUnknown(osName)) osName = "Unknown OS";
            if (isUnknown(deviceClass)) deviceClass = "Device";

            if ("Unknown Browser".equals(agentName) && "Unknown OS".equals(osName)) {
                return "Unknown Device";
            }

            return agentName + " on " + osName + " (" + deviceClass + ")";
        } catch (Exception e) {
            return "Unknown Device";
        }
    }

    /**
     * Phân tích chi tiết Trình duyệt & Hệ điều hành (Ví dụ: Chrome / Windows 10)
     */
    public String parseBrowserOs(String userAgentHeader) {
        if (userAgentHeader == null || userAgentHeader.isBlank()) {
            return "Unknown OS";
        }

        try {
            UserAgent agent = uaa.parse(userAgentHeader);

            String agentName = agent.getValue(UserAgent.AGENT_NAME);
            String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
            String osVersion = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR);

            if (isUnknown(agentName)) agentName = "Unknown Browser";
            if (isUnknown(osName)) osName = "Unknown OS";

            if (!isUnknown(osVersion)) {
                return agentName + " / " + osName + " " + osVersion;
            }

            return agentName + " / " + osName;
        } catch (Exception e) {
            return "Unknown OS";
        }
    }

    /**
     * Hàm tiện ích parse đồng thời 2 thông tin chỉ trong 1 lần duy nhất
     */
    public Map<String, String> parseUserAgent(String userAgentHeader) {
        Map<String, String> result = new HashMap<>();

        if (userAgentHeader == null || userAgentHeader.isBlank()) {
            result.put("deviceName", "Unknown Device");
            result.put("browserOs", "Unknown OS");
            return result;
        }

        try {
            UserAgent agent = uaa.parse(userAgentHeader);

            String agentName = agent.getValue(UserAgent.AGENT_NAME);
            String osName = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
            String deviceClass = agent.getValue(UserAgent.DEVICE_CLASS);
            String osVersion = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR);

            String cleanAgent = isUnknown(agentName) ? "Unknown Browser" : agentName;
            String cleanOs = isUnknown(osName) ? "Unknown OS" : osName;
            String cleanClass = isUnknown(deviceClass) ? "Device" : deviceClass;

            String deviceName = cleanAgent + " on " + cleanOs + " (" + cleanClass + ")";
            String browserOs = !isUnknown(osVersion)
                    ? cleanAgent + " / " + cleanOs + " " + osVersion
                    : cleanAgent + " / " + cleanOs;

            result.put("deviceName", deviceName);
            result.put("browserOs", browserOs);
        } catch (Exception e) {
            result.put("deviceName", "Unknown Device");
            result.put("browserOs", "Unknown OS");
        }

        return result;
    }

    private boolean isUnknown(String value) {
        return value == null || "Unknown".equalsIgnoreCase(value) || "??".equals(value) || value.isBlank();
    }
}