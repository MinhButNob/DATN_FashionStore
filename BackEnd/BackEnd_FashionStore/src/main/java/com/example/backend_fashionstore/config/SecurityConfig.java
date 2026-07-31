package com.example.backend_fashionstore.config;

import com.example.backend_fashionstore.security.CustomUserDetailsService;
import com.example.backend_fashionstore.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authProvider())
                .authorizeHttpRequests(auth -> auth
                        // ===== PUBLIC ENDPOINTS =====
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ===== CLIENT ENDPOINTS (Khách hàng) =====
                        .requestMatchers("/api/customer/home/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/colors/**").permitAll()
                        .requestMatchers("/api/sizes/**").permitAll()
                        .requestMatchers("/api/promotions/active/**").permitAll()
                        .requestMatchers("/api/banners/active/**").permitAll()
                        .requestMatchers("/api/reviews/product/**").permitAll()
                        .requestMatchers("/api/reviews/product-all/**").permitAll()
                        .requestMatchers("/api/vouchers/available/**").permitAll()

                        // ===== CUSTOMER AUTHENTICATED =====
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/customers/profile/**").authenticated()
                        .requestMatchers("/api/wishlist/**").authenticated()
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()

                        // ===== ADMIN & STAFF SHARED ENDPOINTS =====
                        .requestMatchers("/api/admin/orders/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/admin/products/**").hasAnyRole("ADMIN", "STAFF")

                        // ===== ADMIN ENDPOINTS (Chỉ ADMIN) =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== STAFF ENDPOINTS (ADMIN + STAFF) =====
                        .requestMatchers("/api/staff/**").hasAnyRole("ADMIN", "STAFF")

                        // ===== SALE STAFF ENDPOINTS (ADMIN + STAFF + SALE_STAFF) =====
                        .requestMatchers("/api/sale-staff/**").hasAnyRole("ADMIN", "STAFF", "SALE_STAFF")

                        // ===== POS ENDPOINTS (ADMIN + STAFF + SALE_STAFF) =====
                        .requestMatchers("/api/pos/**").hasAnyRole("ADMIN", "STAFF", "SALE_STAFF")

                        // ===== USER ENDPOINTS =====
                        .requestMatchers("/api/user/**").authenticated()

                        // ===== ALL OTHER REQUESTS =====
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}