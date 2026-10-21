package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.request.AsignarTecnicoRequest;
import com.cibertec.helpdesk.dto.request.CambiarEstadoRequest;
import com.cibertec.helpdesk.dto.request.CrearTicketRequest;
import com.cibertec.helpdesk.dto.response.HistorialTicketResponse;
import com.cibertec.helpdesk.dto.response.TicketResponse;
import com.cibertec.helpdesk.service.HistorialTicketService;
import com.cibertec.helpdesk.service.TicketService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

        private final TicketService ticketService;
        private final HistorialTicketService historialTicketService;

        public TicketController(
                        TicketService ticketService,
                        HistorialTicketService historialTicketService) {

                this.ticketService = ticketService;
                this.historialTicketService = historialTicketService;
        }

        // Bandeja general: ADMIN ve todo, TECNICO sus asignados,
        // ESTUDIANTE/DOCENTE sus propios tickets.
        @GetMapping
        public ResponseEntity<List<TicketResponse>> listarTickets(
                        Authentication authentication) {

                return ResponseEntity.ok(
                                ticketService.listarTickets(
                                                authentication.getName()));
        }

        // Bandeja "mis tickets" (ESTUDIANTE / DOCENTE)
        @GetMapping("/mios")
        public ResponseEntity<List<TicketResponse>> listarTicketsMios(
                        Authentication authentication) {

                return ResponseEntity.ok(
                                ticketService.listarTicketsMios(
                                                authentication.getName()));
        }

        // Consultar un ticket por ID
        @GetMapping("/{id}")
        public ResponseEntity<TicketResponse> buscarPorId(
                        @PathVariable Integer id,
                        Authentication authentication) {

                return ResponseEntity.ok(
                                ticketService.buscarPorId(
                                                id,
                                                authentication.getName()));
        }

        // Registrar un ticket con el usuario autenticado
        @PostMapping
        public ResponseEntity<TicketResponse> registrarTicket(
                        @Valid @RequestBody CrearTicketRequest request,
                        Authentication authentication) {

                TicketResponse creado = ticketService.registrarTicket(
                                request,
                                authentication.getName());

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(creado);
        }

        // Cambiar el estado de un ticket
        @PatchMapping("/{id}/estado")
        public ResponseEntity<TicketResponse> cambiarEstado(
                        @PathVariable Integer id,
                        @Valid @RequestBody CambiarEstadoRequest request,
                        Authentication authentication) {

                return ResponseEntity.ok(
                                ticketService.cambiarEstado(
                                                id,
                                                request,
                                                authentication.getName()));
        }

        // Asignar un tecnico a un ticket (solo ADMINISTRADOR)
        @PatchMapping("/{id}/asignar")
        public ResponseEntity<TicketResponse> asignarTecnico(
                        @PathVariable Integer id,
                        @Valid @RequestBody AsignarTecnicoRequest request) {

                return ResponseEntity.ok(
                                ticketService.asignarTecnico(id, request));
        }

        // Bandeja de tickets asignados al tecnico autenticado
        @GetMapping("/asignados")
        public ResponseEntity<List<TicketResponse>> listarAsignados(
                        Authentication authentication) {

                return ResponseEntity.ok(
                                ticketService.listarTicketsAsignados(
                                                authentication.getName()));
        }

        // Historial (bitácora) de un ticket
        @GetMapping("/{id}/historial")
        public ResponseEntity<List<HistorialTicketResponse>> listarHistorial(
                        @PathVariable Integer id,
                        Authentication authentication) {

                return ResponseEntity.ok(
                                historialTicketService.listarHistorial(
                                                id,
                                                authentication.getName()));
        }
}
