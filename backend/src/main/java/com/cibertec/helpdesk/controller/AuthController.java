package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.request.LoginRequest;
import com.cibertec.helpdesk.dto.response.LoginResponse;
import com.cibertec.helpdesk.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.iniciarSesion(request)
        );
    }
}
