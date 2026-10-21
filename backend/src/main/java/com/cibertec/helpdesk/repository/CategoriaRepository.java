package com.cibertec.helpdesk.repository;

import com.cibertec.helpdesk.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    /** El nombre es único sin distinguir mayúsculas (índice uq_categorias_nombre). */
    boolean existsByNombreIgnoreCase(String nombre);
}
