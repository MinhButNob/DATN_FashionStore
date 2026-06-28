package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.auth.dto.*;
import com.example.backend_fashionstore.auth.repository.*;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.enums.RoleStatus;
import com.example.backend_fashionstore.enums.UserStatus;
import com.example.backend_fashionstore.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .status(UserStatus.ACTIVE)  // ✅ Mặc định ACTIVE khi đăng ký
                .build();

        // save user
        user = userRepository.save(user);

        // create CUSTOMER + CART
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

        // ✅ Kiểm tra user có tồn tại không
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        // ✅ KIỂM TRA STATUS TRƯỚC KHI CHO ĐĂNG NHẬP
        if (user.getStatus() == UserStatus.BANNED) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new RuntimeException("Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new RuntimeException("Tài khoản đã bị xóa. Không thể đăng nhập.");
        }

        // ✅ Chỉ cho phép đăng nhập khi status = ACTIVE
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
        }

        // Xác thực mật khẩu
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            System.out.println(e.getClass().getName());
            System.out.println(e.getMessage());
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
                    u.setStatus(UserStatus.ACTIVE);  // ✅ Mặc định ACTIVE
                    return userRepository.save(u);
                });

        // ✅ KIỂM TRA STATUS CHO GOOGLE LOGIN
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
                    u.setStatus(UserStatus.ACTIVE);  // ✅ Mặc định ACTIVE
                    return userRepository.save(u);
                });

        // ✅ KIỂM TRA STATUS CHO FACEBOOK LOGIN
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
            throw new RuntimeException("Tài khoản chưa được kích hoạt.");
        }

        if (user.getAvatarUrl() == null && finalAvatarUrl != null) {
            user.setAvatarUrl(finalAvatarUrl);
            userRepository.save(user);
        }

        ensureSocialAccount(user, name, "FACEBOOK");

        return buildAuthResponse(user);
    }

    private void ensureCustomerAccount(User user, String customerName, String phone) {

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
        }

        // 3. Tạo Customer
        Customer customer = customerRepository
                .findByUser_UserId(user.getUserId())
                .orElse(null);

        if (customer == null) {
            customer = Customer.builder()
                    .user(user)
                    .customerName(customerName)
                    .phone(phone)
                    .build();
            customer = customerRepository.save(customer);
        }

        // 4. Tạo Cart
        if (!cartRepository.existsByCustomer_CustomerId(customer.getCustomerId())) {
            Cart cart = Cart.builder()
                    .customer(customer)
                    .build();
            cartRepository.save(cart);
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

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
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