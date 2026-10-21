package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.request.CategoriaRequest;
import com.cibertec.helpdesk.dto.response.CategoriaResponse;
import com.cibertec.helpdesk.entity.Categoria;
import com.cibertec.helpdesk.mapper.CategoriaMapper;
import com.cibertec.helpdesk.repository.CategoriaRepository;
import com.cibertec.helpdesk.repository.TicketRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TicketRepository ticketRepository;
    private final CategoriaMapper categoriaMapper;

    public CategoriaService(
            CategoriaRepository categoriaRepository,
            TicketRepository ticketRepository,
            CategoriaMapper categoriaMapper) {

        this.categoriaRepository = categoriaRepository;
        this.ticketRepository = ticketRepository;
        this.categoriaMapper = categoriaMapper;
    }

    // Consultar todas las categorias
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaMapper.toResponseList(
                categoriaRepository.findAll());
    }

    // Consultar una categoria por ID
    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Integer id) {
        return categoriaMapper.toResponse(obtenerEntidad(id));
    }

    // Crear una categoria (solo ADMINISTRADOR)
    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {

        String nombre = request.nombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalStateException(
                    "Ya existe una categoria con el nombre: " + nombre);
        }

        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(request.descripcion())
                .build();

        return categoriaMapper.toResponse(
                categoriaRepository.save(categoria));
    }

    // Actualizar una categoria (solo ADMINISTRADOR)
    @Transactional
    public CategoriaResponse actualizar(Integer id, CategoriaRequest request) {

        Categoria categoria = obtenerEntidad(id);
        String nombre = request.nombre().trim();

        boolean cambiaNombre = !categoria.getNombre().equalsIgnoreCase(nombre);

        if (cambiaNombre && categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalStateException(
                    "Ya existe una categoria con el nombre: " + nombre);
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(request.descripcion());

        return categoriaMapper.toResponse(
                categoriaRepository.save(categoria));
    }

    // Eliminar una categoria (solo ADMINISTRADOR)
    @Transactional
    public void eliminar(Integer id) {

        Categoria categoria = obtenerEntidad(id);

        if (ticketRepository.existsByIdCategoria(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar la categoria '" + categoria.getNombre()
                            + "' porque tiene tickets asociados");
        }

        categoriaRepository.delete(categoria);
    }

    private Categoria obtenerEntidad(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Categoria no encontrada: " + id));
    }
}
