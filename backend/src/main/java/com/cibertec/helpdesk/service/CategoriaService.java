package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.entity.Categoria;
import com.cibertec.helpdesk.repository.CategoriaRepository;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // Consultar todas las categorias
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    // Consultar una categoria por ID
    public Categoria buscarPorId(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                    new NoSuchElementException(
                        "Categoria no encontrada: " + id
                    )
                );
    }
}
