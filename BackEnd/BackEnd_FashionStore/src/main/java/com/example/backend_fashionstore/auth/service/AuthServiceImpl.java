package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.auth.dto.*;
import com.example.backend_fashionstore.auth.repository.*;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.RoleStatus;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final GoogleVerifyService googleVerifyService;
    private final FacebookVerifyService facebookVerifyService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("📝 Registering user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (customerRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
        }

        User user = User.builder()
                .username(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("✅ User saved: {}", user.getUserId());

        // ✅ Tạo CUSTOMER + CART với số điện thoại
        ensureCustomerAccount(user, request.getName(), request.getPhone());

        // reload user with roles
        User savedUser = userRepository
                .findByEmailWithRoles(user.getEmail())
                .orElseThrow();

        return buildAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("Tài khoản của bạn đã bị vô hiệu hóa.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Tài khoản đã bị xóa. Không thể đăng nhập.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            log.error("Authentication failed for email: {}", request.getEmail());
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(String idToken) {
        GoogleIdToken.Payload payload = googleVerifyService.verify(idToken);

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepository
                .findByEmailWithRoles(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setUsername(name);
                    u.setAvatarUrl(picture);
                    u.setProvider("GOOGLE");
                    u.setProviderId(payload.getSubject());
                    u.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(u);
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt.");
        }

        ensureSocialAccount(user, name, "GOOGLE");

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse facebookLogin(String accessToken) {
        FacebookUserInfo info = facebookVerifyService.verify(accessToken);

        if (info == null || info.getEmail() == null || info.getEmail().isBlank()) {
            throw new RuntimeException("Invalid Facebook token or missing email");
        }

        String avatarUrl = null;
        if (info.getPicture() != null && info.getPicture().getData() != null) {
            avatarUrl = info.getPicture().getData().getUrl();
        }

        final String finalAvatarUrl = avatarUrl;
        String email = info.getEmail();
        String name = info.getName();
        String facebookId = info.getId();

        User user = userRepository
                .findByEmailWithRoles(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setUsername(name);
                    u.setAvatarUrl(finalAvatarUrl);
                    u.setProvider("FACEBOOK");
                    u.setProviderId(facebookId);
                    u.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(u);
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt.");
        }

        if (user.getAvatarUrl() == null && finalAvatarUrl != null) {
            user.setAvatarUrl(finalAvatarUrl);
            userRepository.save(user);
        }

        ensureSocialAccount(user, name, "FACEBOOK");

        return buildAuthResponse(user);
    }

    /**
     * ✅ ĐẢM BẢO TÀI KHOẢN KHÁCH HÀNG - LƯU SỐ ĐIỆN THOẠI
     */
    private void ensureCustomerAccount(User user, String customerName, String phone) {
        log.info("📦 Ensuring customer account for user: {}, phone: {}", user.getUserId(), phone);

        // 1. Role CUSTOMER
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("CUSTOMER")
                                .status(RoleStatus.ACTIVE)
                                .build()
                ));

        // 2. Gán role
        if (!userRoleRepository.existsByUser_UserIdAndRole_RoleName(user.getUserId(), "CUSTOMER")) {
            userRoleRepository.save(
                    UserRole.builder()
                            .id(new UserRoleId(user.getUserId(), customerRole.getRoleId()))
                            .user(user)
                            .role(customerRole)
                            .build()
            );
            log.info("✅ Assigned CUSTOMER role to user: {}", user.getUserId());
        }

        // 3. Tạo Customer - ✅ LƯU SỐ ĐIỆN THOẠI
        Customer customer = customerRepository
                .findByUser_UserId(user.getUserId())
                .orElse(null);

        if (customer == null) {
            customer = Customer.builder()
                    .user(user)
                    .customerName(customerName)
                    .phone(phone != null && !phone.isBlank() ? phone : null)
                    .build();
            customer = customerRepository.save(customer);
            log.info("✅ Created new customer with phone: {}", customer.getPhone());
        } else {
            // Nếu đã tồn tại nhưng chưa có số điện thoại thì cập nhật
            if (phone != null && !phone.isBlank() &&
                    (customer.getPhone() == null || customer.getPhone().isBlank())) {
                customer.setPhone(phone);
                customer = customerRepository.save(customer);
                log.info("✅ Updated existing customer with phone: {}", customer.getPhone());
            }
        }

        // 4. Tạo Cart
        if (!cartRepository.existsByCustomer_CustomerId(customer.getCustomerId())) {
            Cart cart = Cart.builder()
                    .customer(customer)
                    .build();
            cartRepository.save(cart);
            log.info("✅ Created cart for customer: {}", customer.getCustomerId());
        }
    }

    private void ensureSocialAccount(User user, String displayName, String provider) {
        if (user.getProvider() == null || user.getProvider().isBlank()) {
            user.setProvider(provider);
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(displayName);
        }

        userRepository.save(user);

        ensureCustomerAccount(user, user.getUsername(), null);
    }

    /**
     * ✅ BUILD AUTH RESPONSE - THÊM customerId VÀ phone
     */
    private AuthResponse buildAuthResponse(User user) {
        User authUser = userRepository
                .findByEmailWithRoles(user.getEmail())
                .orElse(user);

        String token = jwtService.generateToken(authUser.getEmail());

        List<String> roles = authUser.getUserRoles() == null
                ? List.of()
                : authUser.getUserRoles()
                .stream()
                .map(role -> role.getRole().getRoleName())
                .toList();

        // ✅ Lấy customerId và phone
        Integer customerId = null;
        String phone = null;
        try {
            Customer customer = customerRepository.findByUser_UserId(authUser.getUserId()).orElse(null);
            if (customer != null) {
                customerId = customer.getCustomerId();
                phone = customer.getPhone();
            }
        } catch (Exception e) {
            log.warn("Cannot get customer info: {}", e.getMessage());
        }

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .customerId(customerId)
                .phone(phone)
                .user(
                        UserResponse.builder()
                                .id(authUser.getUserId())
                                .username(authUser.getUsername())
                                .email(authUser.getEmail())
                                .avatarUrl(authUser.getAvatarUrl())
                                .roles(roles)
                                .build()
                )
                .build();
    }
}