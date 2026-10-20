package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.response.UsuarioResponse;
import com.cibertec.helpdesk.service.UsuarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Consultar todos los usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(
                usuarioService.listarUsuarios()
        );
    }

    // Consultar un usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(
            @PathVariable Integer id) {

        try {
            return ResponseEntity.ok(
                    usuarioService.buscarPorId(id)
            );

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
