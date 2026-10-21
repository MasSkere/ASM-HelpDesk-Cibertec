package com.cibertec.helpdesk.mapper;

import com.cibertec.helpdesk.dto.response.CategoriaResponse;
import com.cibertec.helpdesk.entity.Categoria;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Conversión Entity ⇄ DTO para categorías.
 * Mapeo manual en {@code @Component} (permitido por INSTRUCCIONES_AGENTE.md §3).
 * No contiene lógica de negocio.
 */
@Component
public class CategoriaMapper {

    public CategoriaResponse toResponse(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaResponse(
                categoria.getIdCategoria(),
                categoria.getNombre(),
                categoria.getDescripcion()
        );
    }

    public List<CategoriaResponse> toResponseList(List<Categoria> categorias) {
        return categorias.stream()
                .map(this::toResponse)
                .toList();
    }
}
