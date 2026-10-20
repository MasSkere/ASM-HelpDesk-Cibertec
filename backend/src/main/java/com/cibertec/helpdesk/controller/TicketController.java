package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.response.HistorialTicketResponse;
import com.cibertec.helpdesk.service.HistorialTicketService;
import com.cibertec.helpdesk.dto.request.AsignarTecnicoRequest;
import com.cibertec.helpdesk.dto.request.CambiarEstadoRequest;
import com.cibertec.helpdesk.dto.request.CrearTicketRequest;
import com.cibertec.helpdesk.entity.Ticket;
import com.cibertec.helpdesk.entity.Usuario;
import com.cibertec.helpdesk.repository.UsuarioRepository;
import com.cibertec.helpdesk.service.TicketService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

        private final TicketService ticketService;
        private final UsuarioRepository usuarioRepository;
        private final HistorialTicketService historialTicketService;

        public TicketController(
                        TicketService ticketService,
                        UsuarioRepository usuarioRepository,
                        HistorialTicketService historialTicketService) {

                this.ticketService = ticketService;
                this.usuarioRepository = usuarioRepository;
                this.historialTicketService = historialTicketService;
        }

        // Consultar todos los tickets

        @GetMapping
        public ResponseEntity<List<Ticket>> listarTickets(
                        Authentication authentication) {

                return ResponseEntity.ok(
                                ticketService.listarTickets(
                                                authentication.getName()));
        }

        // Consultar un ticket por ID

        @GetMapping("/{id}")
        public ResponseEntity<Ticket> buscarPorId(
                        @PathVariable Integer id,
                        Authentication authentication) {

                return ResponseEntity.ok(
                                ticketService.buscarPorId(
                                                id,
                                                authentication.getName()));
        }

        // Registrar un ticket con el usuario autenticado
        @PostMapping
        public ResponseEntity<Ticket> registrarTicket(
                        @Valid @RequestBody CrearTicketRequest request,
                        Authentication authentication) {

                String correo = authentication.getName();

                Usuario usuario = usuarioRepository.findByCorreo(correo)
                                .orElseThrow(() -> new NoSuchElementException(
                                                "Usuario autenticado no encontrado"));

                Ticket ticket = ticketService.registrarTicket(
                                request,
                                usuario.getIdUsuario());

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ticket);
        }

        // Cambiar el estado de un ticket

        @PatchMapping("/{id}/estado")
        public ResponseEntity<Ticket> cambiarEstado(
                        @PathVariable Integer id,
                        @Valid @RequestBody CambiarEstadoRequest request,
                        Authentication authentication) {

                Ticket actualizado = ticketService.cambiarEstado(
                                id,
                                request,
                                authentication.getName());

                return ResponseEntity.ok(actualizado);
        }

        // Asignar un tecnico a un ticket
        @PatchMapping("/{id}/asignar")
        public ResponseEntity<Ticket> asignarTecnico(
                        @PathVariable Integer id,
                        @Valid @RequestBody AsignarTecnicoRequest request) {

                try {
                        Ticket ticket = ticketService.asignarTecnico(id, request);
                        return ResponseEntity.ok(ticket);

                } catch (NoSuchElementException e) {
                        return ResponseEntity.notFound().build();
                }
        }

        // Bandeja de tickets asignados al tecnico autenticado
        @GetMapping("/asignados")
        public ResponseEntity<List<Ticket>> listarAsignados(
                        Authentication authentication) {

                List<Ticket> tickets = ticketService.listarTicketsAsignados(
                                authentication.getName());

                return ResponseEntity.ok(tickets);
        }

        @GetMapping("/{id}/historial")
        public ResponseEntity<List<HistorialTicketResponse>> listarHistorial(
                        @PathVariable Integer id,
                        Authentication authentication) {

                List<HistorialTicketResponse> historial = historialTicketService.listarHistorial(
                                id,
                                authentication.getName());

                return ResponseEntity.ok(historial);
        }

}
