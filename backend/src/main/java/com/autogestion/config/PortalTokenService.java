package com.autogestion.config;

import com.autogestion.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Token de sesión del portal del cliente.
 *
 * Es un JWT distinto al del personal: lleva claim tipo=CLIENTE, el subject es "c:&lt;clienteId&gt;"
 * y vence rápido. El filtro JWT le da SOLO el rol CLIENTE, que no abre ningún endpoint del personal.
 * El id del cliente se toma siempre del token, nunca del request.
 */
@Component
public class PortalTokenService {

    public static final String TIPO_CLAIM = "tipo";
    public static final String TIPO_CLIENTE = "CLIENTE";
    public static final String PREFIJO = "c:";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.portal.token-minutos:30}")
    private long minutos;

    public long getMinutos() {
        return minutos;
    }

    public String generar(Long clienteId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date ahora = new Date();
        return Jwts.builder()
                .subject(PREFIJO + clienteId)
                .claim(TIPO_CLAIM, TIPO_CLIENTE)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + minutos * 60_000L))
                .signWith(key)
                .compact();
    }

    /** Id del cliente autenticado (sale del token ya validado por el filtro). */
    public static Long clienteIdDe(Authentication auth) {
        if (auth == null || auth.getName() == null || !auth.getName().startsWith(PREFIJO)) {
            throw new BusinessException("Sesión no válida", HttpStatus.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(auth.getName().substring(PREFIJO.length()));
        } catch (NumberFormatException e) {
            throw new BusinessException("Sesión no válida", HttpStatus.UNAUTHORIZED);
        }
    }
}
