package com.cibertec.helpdesk.repository;

import com.cibertec.helpdesk.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de {@link Usuario}.
 * Vacío a propósito: los programadores backend definen las consultas aquí.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}