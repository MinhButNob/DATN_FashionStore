package com.example.backend_fashionstore.auth.service;

import com.example.backend_fashionstore.auth.dto.*;
import com.example.backend_fashionstore.auth.repository.*;
import com.example.backend_fashionstore.entity.*;
import com.example.backend_fashionstore.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl
        implements AuthService {

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
    public AuthResponse register(
            RegisterRequest request
    ) {

        if (userRepository.existsByEmail(
                request.getEmail()
        )) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        User user = User.builder()
                .username(request.getName())
                .email(request.getEmail())
                .passwordHash(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .provider("LOCAL")
                .build();

        userRepository.save(user);

        ensureCustomerAccount(user, request.getName());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(
            LoginRequest request
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user =
                userRepository.findByEmailWithRoles(
                        request.getEmail()
                ).orElseThrow();

        return buildAuthResponse(user);
    }


    @Override
    @Transactional
    public AuthResponse googleLogin(String idToken) {

        GoogleIdToken.Payload payload = googleVerifyService.verify(idToken);

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepository.findByEmailWithRoles(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setUsername(name);
                    u.setAvatarUrl(picture);
                    u.setProvider("GOOGLE");
                    u.setProviderId(payload.getSubject());
                    return userRepository.save(u);
                });

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

        User user = userRepository.findByEmailWithRoles(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setUsername(name);
                    u.setAvatarUrl(finalAvatarUrl);
                    u.setProvider("FACEBOOK");
                    u.setProviderId(facebookId);
                    return userRepository.save(u);
                });

        if (user.getAvatarUrl() == null && finalAvatarUrl != null) {
            user.setAvatarUrl(finalAvatarUrl);
            userRepository.save(user);
        }

        ensureSocialAccount(user, name, "FACEBOOK");

        return buildAuthResponse(user);
    }

    private void ensureCustomerAccount(User user, String customerName) {
        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("CUSTOMER")
                        .build()));

        if (!userRoleRepository.existsByUser_UserIdAndRole_RoleName(user.getUserId(), "CUSTOMER")) {
            UserRole userRole = UserRole.builder()
                    .id(new UserRoleId(user.getUserId(), customerRole.getRoleId()))
                    .user(user)
                    .role(customerRole)
                    .build();
            userRoleRepository.save(userRole);
        }

        Customer customer = customerRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .user(user)
                        .customerName(customerName)
                        .build()));

        boolean hasCart = cartRepository.existsByCustomer_CustomerId(customer.getCustomerId());
        if (!hasCart) {
            cartRepository.save(Cart.builder().customer(customer).build());
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

        ensureCustomerAccount(user, user.getUsername());
    }

    private AuthResponse buildAuthResponse(User user) {
        User authUser = userRepository.findByEmailWithRoles(user.getEmail())
                .orElse(user);

        String token = jwtService.generateToken(authUser.getEmail());

        List<String> roles = authUser.getUserRoles()
                .stream()
                .map(role -> role.getRole().getRoleName())
                .toList();

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(UserResponse.builder()
                        .id(authUser.getUserId())
                        .username(authUser.getUsername())
                        .email(authUser.getEmail())
                        .avatarUrl(authUser.getAvatarUrl())
                        .roles(roles)
                        .build())
                .build();
    }


}

