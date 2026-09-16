package com.diversao.backend.user;

import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ConflictException;
import com.diversao.backend.exception.ErrorMessages;
import com.diversao.backend.security.JwtService;
import com.diversao.backend.user.AuthResponse;
import com.diversao.backend.user.LoginRequest;
import com.diversao.backend.user.RegisterRequest;
import com.diversao.backend.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.diversao.backend.security.LoginRateLimiter;
import com.diversao.backend.exception.InvalidTokenException;
import com.diversao.backend.user.RefreshTokenRequest; // se necessário

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final LoginRateLimiter loginRateLimiter;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(ErrorMessages.EMAIL_ALREADY_IN_USE);
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user = userRepository.save(user);

        String access = jwtService.generateToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        return new AuthResponse(access, refresh, user.getEmail(), user.getName(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        loginRateLimiter.checkAllowed(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException(ErrorMessages.INVALID_CREDENTIALS));
        String access = jwtService.generateToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        return new AuthResponse(access, refresh, user.getEmail(), user.getName(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(ErrorMessages.USER_NOT_FOUND));
    }

    public AuthResponse refresh(String refreshToken) {
        // 1) precisa ser tipo refresh
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        // 2) precisa ser parseável e não-expirado
        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        String newAccess = jwtService.generateToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);
        return new AuthResponse(newAccess, newRefresh, user.getEmail(), user.getName(), user.getRole().name());
    }
}