package com.cibertec.helpdesk.repository;

import com.cibertec.helpdesk.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    /** Corregir duplicados sin distinguir mayúsculas (índice uq_usuarios_correo). */
    boolean existsByCorreoIgnoreCase(String correo);
}
