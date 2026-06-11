package com.example.backend_fashionstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

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
}