package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.auth.dto.FacebookUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class FacebookVerifyService {

    @Value("${spring.security.oauth2.client.registration.facebook.client-id:}")
    private String appId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret:}")
    private String appSecret;

    public FacebookUserInfo verify(String accessToken) {

        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new RuntimeException("Facebook OAuth configuration is missing");
        }

        RestTemplate restTemplate = new RestTemplate();

        String appAccessToken = appId + "|" + appSecret;
        String debugUrl = "https://graph.facebook.com/debug_token?input_token="
                + accessToken
                + "&access_token="
                + appAccessToken;

        Map<?, ?> debugResponse = restTemplate.getForObject(debugUrl, Map.class);
        if (debugResponse == null || !debugResponse.containsKey("data")) {
            throw new RuntimeException("Invalid Facebook token");
        }

        Object dataObject = debugResponse.get("data");
        if (!(dataObject instanceof Map<?, ?> data)
                || !Boolean.TRUE.equals(data.get("is_valid"))) {
            throw new RuntimeException("Invalid Facebook token");
        }

        String meUrl = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token="
                + accessToken;

        FacebookUserInfo userInfo = restTemplate.getForObject(meUrl, FacebookUserInfo.class);
        if (userInfo == null || userInfo.getId() == null || userInfo.getEmail() == null) {
            throw new RuntimeException("Facebook profile data is incomplete");
        }

        return userInfo;
    }
}