package com.cibertec.helpdesk.repository;

import com.cibertec.helpdesk.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de {@link Categoria}.
 * Vacío a propósito: los programadores backend definen las consultas aquí.
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}