package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.request.CategoriaRequest;
import com.cibertec.helpdesk.dto.response.CategoriaResponse;
import com.cibertec.helpdesk.service.CategoriaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(
            CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // GET: consultar todas las categorias (cualquier autenticado)
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(
                categoriaService.listarCategorias());
    }

    // GET: consultar una categoria por su ID
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                categoriaService.buscarPorId(id));
    }

    // POST: crear una categoria (solo ADMINISTRADOR)
    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(
            @Valid @RequestBody CategoriaRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoriaService.crear(request));
    }

    // PUT: actualizar una categoria (solo ADMINISTRADOR)
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CategoriaRequest request) {

        return ResponseEntity.ok(
                categoriaService.actualizar(id, request));
    }

    // DELETE: eliminar una categoria (solo ADMINISTRADOR)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {

        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
