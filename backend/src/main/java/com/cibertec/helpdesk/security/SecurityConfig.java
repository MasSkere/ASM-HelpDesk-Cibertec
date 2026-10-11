package com.cibertec.helpdesk.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración BASE de Spring Security (Fase 2).
 *
 * <p>Deja preparada la seguridad stateless sin sesiones (JWT):
 * <ul>
 *     <li>CSRF deshabilitado (no aplica con JWT).</li>
 *     <li>Sesión STATELESS.</li>
 *     <li>Rutas públicas y privadas definidas por rol.</li>
 *     <li>Encoder BCrypt para contraseñas.</li>
 * </ul>
 *
 * <p><b>TODO Fase 3:</b> registrar el {@code JwtAuthenticationFilter} con
 * {@code .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)}
 * (ver INSTRUCCIONES_AGENTE.md, sección 5).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Rutas accesibles sin autenticación. */
    private static final String[] PUBLIC_ROUTES = {
            "/api/auth/**",
            "/actuator/health",
            "/actuator/info",
            "/error"
    };

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Al usar JWT no se necesitan tokens CSRF ni formularios/sesiones
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // -------- Públicas --------
                        .requestMatchers(PUBLIC_ROUTES).permitAll()

                        // -------- Tickets --------
                        // Estudiante y Docente pueden registrar tickets
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasAnyRole("ESTUDIANTE", "DOCENTE")
                        // Solo Técnico consulta la bandeja de asignados
                        .requestMatchers("/api/tickets/asignados/**").hasRole("TECNICO")
                        // Gestión de tickets (update/estado/asignación): Técnico o Administrador
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/**").hasAnyRole("TECNICO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/**").hasAnyRole("TECNICO", "ADMINISTRADOR")

                        // -------- Usuarios --------
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")

                        // -------- Categorías --------
                        // Lectura: cualquier usuario autenticado. Escritura: Administrador.
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").authenticated()
                        .requestMatchers("/api/categorias/**").hasRole("ADMINISTRADOR")

                        // -------- Resto --------
                        .anyRequest().authenticated()
                );

        // TODO Fase 3: JWT.
        // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        return http.build();
    }

    /** Cifrado de contraseñas con BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Expone el AuthenticationManager para el login (AuthService / AuthController). */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** CORS para el frontend React (orígenes configurables por entorno). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
