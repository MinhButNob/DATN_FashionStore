package com.example.backend_fashionstore.config;

import com.example.backend_fashionstore.chat_nhan_vien.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSocket  // ===== CODE MỚI: Thêm dòng này để bật WebSocket =====
public class WebConfig implements WebMvcConfigurer, WebSocketConfigurer {  // ===== CODE MỚI: Thêm WebSocketConfigurer =====

    // ===== CODE MỚI: Inject WebSocket Handler =====
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    // ===== CODE CŨ: Cấu hình ResourceHandler (GIỮ NGUYÊN) =====
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Cấu hình đúng đường dẫn tuyệt đối
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCachePeriod(3600);

        // ✅ Hoặc dùng đường dẫn tuyệt đối (Windows)
        // .addResourceLocations("file:C:/BackEnd_FashionStore/uploads/")

        // ✅ Nếu dùng Linux/Mac
        // .addResourceLocations("file:/home/user/BackEnd_FashionStore/uploads/")
    }

    // ===== CODE CŨ: Cấu hình CORS (GIỮ NGUYÊN) =====
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    // ===== CODE MỚI: Cấu hình WebSocket Handler =====
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*");
    }
}