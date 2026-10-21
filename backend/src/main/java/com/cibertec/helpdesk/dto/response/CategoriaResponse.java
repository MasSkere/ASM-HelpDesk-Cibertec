package com.cibertec.helpdesk.dto.response;

/** Representación pública de una categoría (nunca se expone la Entity). */
public record CategoriaResponse(
        Integer idCategoria,
        String nombre,
        String descripcion
) {}
