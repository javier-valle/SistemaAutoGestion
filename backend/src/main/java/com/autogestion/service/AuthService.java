package com.autogestion.service;

import com.autogestion.dto.LoginRequest;
import com.autogestion.dto.LoginResponse;
import com.autogestion.entity.Usuario;
import com.autogestion.exception.BusinessException;
import com.autogestion.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Credenciales inválidas",
                        org.springframework.http.HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BusinessException("Credenciales inválidas",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        if (!usuario.getActivo()) {
            throw new BusinessException("Credenciales inválidas",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        String token = generateToken(usuario);

        return LoginResponse.builder()
                .token(token)
                .id(usuario.getId())
                .nombre(usuario.getNombreCompleto())
                .nombreCompleto(usuario.getNombreCompleto())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .build();
    }

    private String generateToken(Usuario usuario) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("rol", usuario.getRol())
                .claim("nombre", usuario.getNombreCompleto())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
