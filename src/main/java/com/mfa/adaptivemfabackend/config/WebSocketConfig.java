package com.mfa.adaptivemfabackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kênh Broadcast tin nhắn đẩy xuống Client (VD: /topic/mfa-notifications)
        config.enableSimpleBroker("/topic");

        // Tiền tố cho các API client gửi lên WebSocket server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký Endpoint kết nối WebSocket hỗ trợ cả SockJS lẫn kết nối Native WebSocket
        registry.addEndpoint("/ws-mfa").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws-mfa").setAllowedOriginPatterns("*");
    }
}