package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.request.AsignarTecnicoRequest;
import com.cibertec.helpdesk.dto.request.CambiarEstadoRequest;
import com.cibertec.helpdesk.dto.request.CrearTicketRequest;
import com.cibertec.helpdesk.dto.response.TicketResponse;
import com.cibertec.helpdesk.entity.EstadoTicket;
import com.cibertec.helpdesk.entity.Rol;
import com.cibertec.helpdesk.entity.Ticket;
import com.cibertec.helpdesk.entity.Usuario;
import com.cibertec.helpdesk.mapper.TicketMapper;
import com.cibertec.helpdesk.repository.CategoriaRepository;
import com.cibertec.helpdesk.repository.TicketRepository;
import com.cibertec.helpdesk.repository.UsuarioRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TicketService {

        private final TicketRepository ticketRepository;
        private final CategoriaRepository categoriaRepository;
        private final UsuarioRepository usuarioRepository;
        private final TicketMapper ticketMapper;

        public TicketService(
                        TicketRepository ticketRepository,
                        CategoriaRepository categoriaRepository,
                        UsuarioRepository usuarioRepository,
                        TicketMapper ticketMapper) {

                this.ticketRepository = ticketRepository;
                this.categoriaRepository = categoriaRepository;
                this.usuarioRepository = usuarioRepository;
                this.ticketMapper = ticketMapper;
        }

        // ------------------------------------------------------------------
        // Consultas
        // ------------------------------------------------------------------

        /** Lista los tickets visibles para el usuario según su rol. */
        @Transactional(readOnly = true)
        public List<TicketResponse> listarTickets(String correoUsuario) {

                Usuario usuario = obtenerUsuario(correoUsuario);

                List<Ticket> tickets = switch (usuario.getRol()) {

                        case ADMINISTRADOR ->
                                ticketRepository.findAll();

                        case TECNICO ->
                                ticketRepository.findByIdTecnico(
                                                usuario.getIdUsuario());

                        case ESTUDIANTE, DOCENTE ->
                                ticketRepository.findByIdSolicitante(
                                                usuario.getIdUsuario());
                };

                return ticketMapper.toResponseList(tickets);
        }

        /** Bandeja "mis tickets" (solo ESTUDIANTE / DOCENTE). */
        @Transactional(readOnly = true)
        public List<TicketResponse> listarTicketsMios(String correoUsuario) {

                Usuario usuario = obtenerUsuario(correoUsuario);

                if (usuario.getRol() != Rol.ESTUDIANTE
                                && usuario.getRol() != Rol.DOCENTE) {
                        throw new AccessDeniedException(
                                        "Solo estudiantes y docentes tienen bandeja de tickets propios");
                }

                return ticketMapper.toResponseList(
                                ticketRepository.findByIdSolicitante(
                                                usuario.getIdUsuario()));
        }

        /** Consulta un ticket por ID respetando la visibilidad del rol. */
        @Transactional(readOnly = true)
        public TicketResponse buscarPorId(Integer id, String correoUsuario) {
                return ticketMapper.toResponse(
                                obtenerEntidadAutorizada(id, correoUsuario));
        }

        /**
         * Verifica que el usuario pueda acceder al ticket. Se usa para autorizar
         * consultas derivadas (por ejemplo el historial).
         */
        @Transactional(readOnly = true)
        public void validarAcceso(Integer id, String correoUsuario) {
                obtenerEntidadAutorizada(id, correoUsuario);
        }

        /** Bandeja de tickets asignados al técnico autenticado. */
        @Transactional(readOnly = true)
        public List<TicketResponse> listarTicketsAsignados(String correoTecnico) {

                Usuario tecnico = obtenerUsuario(correoTecnico);

                if (tecnico.getRol() != Rol.TECNICO) {
                        throw new AccessDeniedException(
                                        "El usuario no tiene el rol TECNICO");
                }

                return ticketMapper.toResponseList(
                                ticketRepository.findByIdTecnico(
                                                tecnico.getIdUsuario()));
        }

        // ------------------------------------------------------------------
        // Escritura
        // ------------------------------------------------------------------

        /** Registra un ticket a nombre del usuario autenticado. */
        @Transactional
        public TicketResponse registrarTicket(
                        CrearTicketRequest request,
                        String correoUsuario) {

                Usuario solicitante = obtenerUsuario(correoUsuario);

                if (request.idCategoria() == null
                                || !categoriaRepository.existsById(request.idCategoria())) {
                        throw new IllegalArgumentException(
                                        "La categoria seleccionada no existe");
                }

                Ticket ticket = Ticket.builder()
                                .asunto(request.asunto())
                                .descripcion(request.descripcion())
                                .tipoSolicitud(request.tipoSolicitud())
                                .prioridad(request.prioridad())
                                .estado(EstadoTicket.REGISTRADO)
                                .idSolicitante(solicitante.getIdUsuario())
                                .idCategoria(request.idCategoria())
                                .build();

                // El codigo TCK-AAAA-NNNN lo genera PostgreSQL mediante el DEFAULT
                // de la columna (secuencia seq_tickets_codigo).
                return ticketMapper.toResponse(ticketRepository.save(ticket));
        }

        /** Cambia el estado de un ticket respetando la máquina de estados. */
        @Transactional
        public TicketResponse cambiarEstado(
                        Integer id,
                        CambiarEstadoRequest request,
                        String correoUsuario) {

                Ticket ticket = ticketRepository.findById(id)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "No se encontro el ticket: " + id));

                Usuario usuario = obtenerUsuario(correoUsuario);

                // Solo administradores o el técnico asignado
                if (usuario.getRol() != Rol.ADMINISTRADOR) {
                        if (usuario.getRol() != Rol.TECNICO
                                        || !usuario.getIdUsuario().equals(ticket.getIdTecnico())) {
                                throw new AccessDeniedException(
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
                        return ticketMapper.toResponse(ticket);
                }

                boolean permitido = switch (actual) {
                        case REGISTRADO ->
                                nuevo == EstadoTicket.EN_PROCESO
                                                || nuevo == EstadoTicket.CANCELADO;

                        case EN_PROCESO ->
                                nuevo == EstadoTicket.RESUELTO
                                                || nuevo == EstadoTicket.CANCELADO;

                        case RESUELTO ->
                                nuevo == EstadoTicket.CERRADO;

                        case CERRADO, CANCELADO -> false;
                };

                if (!permitido) {
                        throw new IllegalStateException(
                                        "No se permite cambiar de " + actual + " a " + nuevo);
                }

                ticket.setEstado(nuevo);

                return ticketMapper.toResponse(ticketRepository.save(ticket));
        }

        /** Asigna un técnico activo a un ticket. */
        @Transactional
        public TicketResponse asignarTecnico(
                        Integer idTicket,
                        AsignarTecnicoRequest request) {

                Ticket ticket = ticketRepository.findById(idTicket)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "No se encontro el ticket: " + idTicket));

                Usuario tecnico = usuarioRepository
                                .findById(request.idTecnico())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "El tecnico seleccionado no existe"));

                if (tecnico.getRol() != Rol.TECNICO) {
                        throw new IllegalArgumentException(
                                        "El usuario seleccionado no es un tecnico");
                }

                if (!Boolean.TRUE.equals(tecnico.getEstado())) {
                        throw new IllegalArgumentException(
                                        "El tecnico seleccionado esta inactivo");
                }

                if (ticket.getEstado() == EstadoTicket.CERRADO
                                || ticket.getEstado() == EstadoTicket.CANCELADO) {
                        throw new IllegalStateException(
                                        "No se puede asignar un ticket cerrado o cancelado");
                }

                ticket.setIdTecnico(tecnico.getIdUsuario());

                return ticketMapper.toResponse(ticketRepository.save(ticket));
        }

        // ------------------------------------------------------------------
        // Helpers privados
        // ------------------------------------------------------------------

        private Usuario obtenerUsuario(String correo) {
                return usuarioRepository.findByCorreo(correo)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Usuario no encontrado"));
        }

        private Ticket obtenerEntidad(Integer id) {
                return ticketRepository.findById(id)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Ticket no encontrado"));
        }

        private Ticket obtenerEntidadAutorizada(Integer id, String correoUsuario) {

                Ticket ticket = obtenerEntidad(id);
                Usuario usuario = obtenerUsuario(correoUsuario);

                boolean autorizado = switch (usuario.getRol()) {

                        case ADMINISTRADOR -> true;

                        case TECNICO ->
                                usuario.getIdUsuario().equals(ticket.getIdTecnico());

                        case ESTUDIANTE, DOCENTE ->
                                usuario.getIdUsuario().equals(ticket.getIdSolicitante());
                };

                if (!autorizado) {
                        // No revelamos si existe un ticket ajeno
                        throw new NoSuchElementException("Ticket no encontrado");
                }

                return ticket;
        }
}
