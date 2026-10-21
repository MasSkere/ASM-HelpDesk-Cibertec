package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.request.LoginRequest;
import com.cibertec.helpdesk.dto.request.RegistroRequest;
import com.cibertec.helpdesk.dto.response.LoginResponse;
import com.cibertec.helpdesk.entity.Rol;
import com.cibertec.helpdesk.entity.Usuario;
import com.cibertec.helpdesk.repository.UsuarioRepository;
import com.cibertec.helpdesk.security.JwtService;
import com.cibertec.helpdesk.security.UsuarioDetailsService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDetailsService usuarioDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            UsuarioDetailsService usuarioDetailsService,
            UsuarioRepository usuarioRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {

        this.authenticationManager = authenticationManager;
        this.usuarioDetailsService = usuarioDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse iniciarSesion(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.correo(),
                        request.password()
                )
        );

        UserDetails userDetails =
                usuarioDetailsService.loadUserByUsername(request.correo());

        Usuario usuario = usuarioRepository
                .findByCorreo(request.correo())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Credenciales incorrectas"
                        )
                );

        String token = jwtService.generarToken(userDetails);

        return new LoginResponse(
                token,
                "Bearer",
                usuario.getCorreo(),
                usuario.getRol().name()
        );
    }

    /**
     * Registro público de un nuevo usuario.
     * Por defecto se crea con rol ESTUDIANTE y estado activo.
     * Devuelve un token para que el cliente pueda iniciar sesión directamente.
     */
    @Transactional
    public LoginResponse registrar(RegistroRequest request) {

        String correo = request.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new IllegalStateException(
                    "Ya existe un usuario registrado con el correo: " + correo);
        }

        Usuario usuario = Usuario.builder()
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .correo(correo)
                .password(passwordEncoder.encode(request.password()))
                .rol(Rol.ESTUDIANTE)
                .estado(true)
                .build();

        usuarioRepository.save(usuario);

        UserDetails userDetails =
                usuarioDetailsService.loadUserByUsername(correo);

        String token = jwtService.generarToken(userDetails);

        return new LoginResponse(
                token,
                "Bearer",
                usuario.getCorreo(),
                usuario.getRol().name()
        );
    }
}
