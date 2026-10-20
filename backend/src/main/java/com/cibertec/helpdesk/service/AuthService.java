package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.request.LoginRequest;
import com.cibertec.helpdesk.dto.response.LoginResponse;
import com.cibertec.helpdesk.entity.Usuario;
import com.cibertec.helpdesk.repository.UsuarioRepository;
import com.cibertec.helpdesk.security.JwtService;
import com.cibertec.helpdesk.security.UsuarioDetailsService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDetailsService usuarioDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UsuarioDetailsService usuarioDetailsService,
            UsuarioRepository usuarioRepository,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.usuarioDetailsService = usuarioDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
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
}
