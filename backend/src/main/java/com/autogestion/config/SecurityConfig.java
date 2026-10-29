package com.autogestion.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /** Consola H2 solo en desarrollo (perfil por defecto). En docker va en false. */
    @Value("${app.h2-console:false}")
    private boolean h2Console;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Desarrollo local: el portal se abre en varios puertos (Docker 5501,
        // Live Server 5500/5501, `python -m http.server` 8000...) y a veces con
        // doble clic sobre index.html, donde el navegador manda Origin "null".
        // Sin estos patrones el navegador bloquea las llamadas a
        // /api/public/portal/documento/consultar y el registro cae en
        // "La fuente de consulta no respondió".
        // En producción el frontend y la API comparten origen (Nginx), así que
        // esta lista no interviene. El token del portal viaja en la cabecera
        // Authorization (no en cookies), por lo que no hay CSRF entre orígenes.
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "null"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(
                new CorsFilter(corsConfigurationSource())
        );
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // Públicas: login del personal e identificación del portal (nada más).
                auth.requestMatchers("/api/auth/**").permitAll();
                auth.requestMatchers(HttpMethod.GET,
                        "/api/public/portal/documento/consultar",
                        "/api/public/portal/info").permitAll();
                auth.requestMatchers(HttpMethod.POST,
                        "/api/public/portal/identificar",
                        "/api/public/portal/registro").permitAll();
                if (h2Console) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                auth.requestMatchers("/api/admin/padron-ruc/**").hasRole("ADMIN");
                // Portal: solo token CLIENTE. API del personal: solo roles del personal
                // (un token de cliente NO abre ningún endpoint del personal).
                auth.requestMatchers("/api/portal/**").hasRole("CLIENTE");
                auth.requestMatchers("/api/**").hasAnyRole("ADMIN", "MECANICO", "RECEPCIONISTA", "ALMACENERO");
                auth.anyRequest().authenticated();
            })
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(headers -> {
                if (h2Console) {
                    headers.frameOptions(frame -> frame.disable());
                } else {
                    headers.frameOptions(frame -> frame.sameOrigin());
                }
            })
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}