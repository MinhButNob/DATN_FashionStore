package com.example.backend_fashionstore.config;

import com.example.backend_fashionstore.chat_nhan_vien.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// ⚠️ CORS KHÔNG cấu hình ở đây nữa - đã chuyển hẳn về SecurityConfig
// (bean CorsConfigurationSource + .cors(Customizer.withDefaults())).
// Lý do: nếu cấu hình CORS ở CẢ WebMvcConfigurer (addCorsMappings) LẪN
// Spring Security cùng lúc, response có thể bị gắn 2 header
// Access-Control-Allow-Origin trùng nhau -> Chrome báo lỗi CORS dù
// origin đã được cho phép đúng. Chỉ nên giữ 1 nơi cấu hình duy nhất.
@Configuration
@EnableWebSocket
public class WebConfig implements WebMvcConfigurer, WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCachePeriod(3600);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins(
                        "https://datn-fe-two.vercel.app",
                        "https://datn-fe-fashionstore.vercel.app",
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:8080",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:5174"
                );
    }
}