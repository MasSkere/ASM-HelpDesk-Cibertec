package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.request.LoginRequest;
import com.cibertec.helpdesk.dto.request.RegistroRequest;
import com.cibertec.helpdesk.dto.response.LoginResponse;
import com.cibertec.helpdesk.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Login de un usuario registrado. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.iniciarSesion(request)
        );
    }

    /** Registro público (rol por defecto: ESTUDIANTE). */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> registrar(
            @Valid @RequestBody RegistroRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.registrar(request));
    }
}
