package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.request.AsignarTecnicoRequest;
import com.cibertec.helpdesk.entity.Usuario;
import com.cibertec.helpdesk.entity.Rol;
import com.cibertec.helpdesk.dto.request.CambiarEstadoRequest;
import com.cibertec.helpdesk.dto.request.CrearTicketRequest;
import com.cibertec.helpdesk.entity.EstadoTicket;
import com.cibertec.helpdesk.entity.Ticket;
import com.cibertec.helpdesk.repository.CategoriaRepository;
import com.cibertec.helpdesk.repository.TicketRepository;
import com.cibertec.helpdesk.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TicketService {

        private final TicketRepository ticketRepository;
        private final CategoriaRepository categoriaRepository;
        private final UsuarioRepository usuarioRepository;

        public TicketService(
                        TicketRepository ticketRepository,
                        CategoriaRepository categoriaRepository,
                        UsuarioRepository usuarioRepository) {

                this.ticketRepository = ticketRepository;
                this.categoriaRepository = categoriaRepository;
                this.usuarioRepository = usuarioRepository;
        }

        // Consultar todos los tickets

        public List<Ticket> listarTickets(String correoUsuario) {

                Usuario usuario = usuarioRepository
                                .findByCorreo(correoUsuario)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Usuario no encontrado"));

                return switch (usuario.getRol()) {

                        case ADMINISTRADOR ->
                                ticketRepository.findAll();

                        case TECNICO ->
                                ticketRepository.findByIdTecnico(
                                                usuario.getIdUsuario());

                        case ESTUDIANTE, DOCENTE ->
                                ticketRepository.findByIdSolicitante(
                                                usuario.getIdUsuario());
                };
        }

        // Consultar un ticket por ID

        public Ticket buscarPorId(
                        Integer id,
                        String correoUsuario) {

                Ticket ticket = ticketRepository.findById(id)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Ticket no encontrado"));

                Usuario usuario = usuarioRepository
                                .findByCorreo(correoUsuario)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Usuario no encontrado"));

                boolean autorizado = switch (usuario.getRol()) {

                        case ADMINISTRADOR -> true;

                        case TECNICO ->
                                usuario.getIdUsuario().equals(
                                                ticket.getIdTecnico());

                        case ESTUDIANTE, DOCENTE ->
                                usuario.getIdUsuario().equals(
                                                ticket.getIdSolicitante());
                };

                if (!autorizado) {
                        // No revelamos si existe un ticket ajeno
                        throw new NoSuchElementException(
                                        "Ticket no encontrado");
                }

                return ticket;
        }

        // Registrar un nuevo ticket
        @Transactional
        public Ticket registrarTicket(
                        CrearTicketRequest request,
                        Integer idSolicitante) {

                // Verificar que el usuario exista
                if (idSolicitante == null ||
                                !usuarioRepository.existsById(idSolicitante)) {
                        throw new IllegalArgumentException(
                                        "El usuario solicitante no existe");
                }

                // Verificar que la categoria exista
                if (request.idCategoria() == null ||
                                !categoriaRepository.existsById(request.idCategoria())) {
                        throw new IllegalArgumentException(
                                        "La categoria seleccionada no existe");
                }

                // Generar codigo de identificacion

                // Construir el nuevo ticket
                Ticket ticket = Ticket.builder()
                                .asunto(request.asunto())
                                .descripcion(request.descripcion())
                                .tipoSolicitud(request.tipoSolicitud())
                                .prioridad(request.prioridad())
                                .estado(EstadoTicket.REGISTRADO)
                                .idSolicitante(idSolicitante)
                                .idCategoria(request.idCategoria())
                                .build();

                // Guardar en PostgreSQL
                return ticketRepository.save(ticket);
        }

        // Actualizar el estado de un ticket

        @Transactional
        public Ticket cambiarEstado(
                        Integer id,
                        CambiarEstadoRequest request,
                        String correoUsuario) {

                Ticket ticket = ticketRepository.findById(id)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "No se encontro el ticket: " + id));

                Usuario usuario = usuarioRepository
                                .findByCorreo(correoUsuario)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Usuario no encontrado"));

                // Solo administradores o tecnicos asignados
                if (usuario.getRol() != Rol.ADMINISTRADOR) {

                        if (usuario.getRol() != Rol.TECNICO ||
                                        !usuario.getIdUsuario().equals(ticket.getIdTecnico())) {

                                throw new org.springframework.security.access.AccessDeniedException(
                                                "No tienes permiso para modificar este ticket");
                        }
                }

                EstadoTicket actual = ticket.getEstado();
                EstadoTicket nuevo = request.estado();

                if (nuevo == null) {
                        throw new IllegalArgumentException(
                                        "El nuevo estado es obligatorio");
                }

                if (actual == nuevo) {
                        return ticket;
                }

                boolean permitido = switch (actual) {
                        case REGISTRADO ->
                                nuevo == EstadoTicket.EN_PROCESO ||
                                                nuevo == EstadoTicket.CANCELADO;

                        case EN_PROCESO ->
                                nuevo == EstadoTicket.RESUELTO ||
                                                nuevo == EstadoTicket.CANCELADO;

                        case RESUELTO ->
                                nuevo == EstadoTicket.CERRADO;

                        case CERRADO, CANCELADO -> false;
                };

                if (!permitido) {
                        throw new IllegalStateException(
                                        "No se permite cambiar de " +
                                                        actual + " a " + nuevo);
                }

                ticket.setEstado(nuevo);

                return ticketRepository.save(ticket);
        }

        // Asignar un tecnico a un ticket
        @Transactional
        public Ticket asignarTecnico(
                        Integer idTicket,
                        AsignarTecnicoRequest request) {

                Ticket ticket = ticketRepository.findById(idTicket)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "No se encontro el ticket: " + idTicket));

                Usuario tecnico = usuarioRepository
                                .findById(request.idTecnico())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El tecnico seleccionado no existe"));

                // Comprobar que tenga el rol TECNICO
                if (tecnico.getRol() != Rol.TECNICO) {
                        throw new IllegalArgumentException(
                                        "El usuario seleccionado no es un tecnico");
                }

                // Comprobar que este activo
                if (!Boolean.TRUE.equals(tecnico.getEstado())) {
                        throw new IllegalArgumentException(
                                        "El tecnico seleccionado esta inactivo");
                }

                // Evitar asignar tickets cerrados o cancelados
                if (ticket.getEstado() == EstadoTicket.CERRADO ||
                                ticket.getEstado() == EstadoTicket.CANCELADO) {
                        throw new IllegalStateException(
                                        "No se puede asignar un ticket cerrado o cancelado");
                }

                ticket.setIdTecnico(tecnico.getIdUsuario());

                return ticketRepository.save(ticket);
        }

        // Consultar los tickets de un tecnico
        public List<Ticket> listarTicketsAsignados(
                        String correoTecnico) {

                Usuario tecnico = usuarioRepository
                                .findByCorreo(correoTecnico)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Tecnico no encontrado"));

                if (tecnico.getRol() != Rol.TECNICO) {
                        throw new IllegalArgumentException(
                                        "El usuario no tiene el rol TECNICO");
                }

                return ticketRepository.findByIdTecnico(
                                tecnico.getIdUsuario());
        }

}
