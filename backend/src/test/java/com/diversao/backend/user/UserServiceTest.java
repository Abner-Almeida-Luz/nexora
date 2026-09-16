package com.diversao.backend.user;

import com.diversao.backend.exception.BadRequestException;
import com.diversao.backend.exception.ConflictException;
import com.diversao.backend.security.JwtService;
import com.diversao.backend.user.AuthResponse;
import com.diversao.backend.user.LoginRequest;
import com.diversao.backend.user.RegisterRequest;
import com.diversao.backend.user.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de UserService: registro, login e busca por e-mail.
 * Não sobe contexto Spring — todo o comportamento é verificado com mocks.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserMapper userMapper;

    @InjectMocks UserService userService;

    @Test
    @DisplayName("register: cria usuário, encoda a senha e devolve token")
    void register_happyPath() {
        var request = new RegisterRequest("Ana", "ana@x.com", "secret1");
        var mapped = User.builder().name("Ana").email("ana@x.com").role(Role.USER).build();

        when(userRepository.existsByEmail("ana@x.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mapped);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = userService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals("ana@x.com", response.email());
        assertEquals("USER", response.role());
        assertEquals("hashed", mapped.getPassword());
        verify(userRepository).save(mapped);
    }

    @Test
    @DisplayName("register: e-mail duplicado lança ConflictException")
    void register_emailDuplicado_lancaConflictException() {
        var request = new RegisterRequest("Ana", "ana@x.com", "secret1");
        when(userRepository.existsByEmail("ana@x.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("login: credenciais válidas devolvem token")
    void login_happyPath() {
        var request = new LoginRequest("ana@x.com", "secret1");
        var user = User.builder().id(1L).name("Ana").email("ana@x.com").role(Role.USER).build();

        when(userRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = userService.login(request);

        assertEquals("jwt-token", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: usuário ausente após auth lança BadRequestException (defensivo)")
    void login_usuarioAusenteAposAuth_lancaBadRequestException() {
        var request = new LoginRequest("ghost@x.com", "secret1");
        when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("getByEmail: devolve o usuário encontrado")
    void getByEmail_happyPath() {
        var user = User.builder().id(1L).email("ana@x.com").build();
        when(userRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(user));

        assertSame(user, userService.getByEmail("ana@x.com"));
    }

    @Test
    @DisplayName("getByEmail: não encontrado lança BadRequestException (ver ambiguidade #2)")
    void getByEmail_naoEncontrado_lancaBadRequestException() {
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> userService.getByEmail("x@x.com"));
    }
}