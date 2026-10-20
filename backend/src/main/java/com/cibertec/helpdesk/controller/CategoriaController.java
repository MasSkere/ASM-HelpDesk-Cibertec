package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.entity.Categoria;
import com.cibertec.helpdesk.service.CategoriaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(
            CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // GET: consultar todas las categorias
    @GetMapping
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(
                categoriaService.listarCategorias()
        );
    }

    // GET: consultar una categoria por su ID
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscarPorId(
            @PathVariable Integer id) {

        try {
            return ResponseEntity.ok(
                    categoriaService.buscarPorId(id)
            );
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
