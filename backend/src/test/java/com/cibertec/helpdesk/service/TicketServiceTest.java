package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.request.AsignarTecnicoRequest;
import com.cibertec.helpdesk.dto.request.CambiarEstadoRequest;
import com.cibertec.helpdesk.entity.EstadoTicket;
import com.cibertec.helpdesk.entity.Rol;
import com.cibertec.helpdesk.entity.Ticket;
import com.cibertec.helpdesk.entity.Usuario;
import com.cibertec.helpdesk.repository.CategoriaRepository;
import com.cibertec.helpdesk.repository.TicketRepository;
import com.cibertec.helpdesk.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

        @Mock
        private TicketRepository ticketRepository;

        @Mock
        private CategoriaRepository categoriaRepository;

        @Mock
        private UsuarioRepository usuarioRepository;

        @InjectMocks
        private TicketService ticketService;

        @Test
        void estudiantePuedeConsultarSuTicket() {

                String correo = "estudiante.prueba@example.com";

                Ticket ticket = new Ticket();
                ticket.setIdTicket(1);
                ticket.setAsunto("Problema con el Wi-Fi");
                ticket.setIdSolicitante(1);

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(1);
                usuario.setCorreo(correo);
                usuario.setRol(Rol.ESTUDIANTE);

                when(ticketRepository.findById(1))
                                .thenReturn(Optional.of(ticket));

                when(usuarioRepository.findByCorreo(correo))
                                .thenReturn(Optional.of(usuario));

                Ticket resultado = ticketService.buscarPorId(1, correo);

                assertNotNull(resultado);
                assertEquals(1, resultado.getIdTicket());
                assertEquals("Problema con el Wi-Fi", resultado.getAsunto());

                verify(ticketRepository).findById(1);
                verify(usuarioRepository).findByCorreo(correo);
        }

        @Test
        void estudianteNoPuedeConsultarTicketAjeno() {

                String correo = "estudiante2.prueba@example.com";

                Ticket ticket = new Ticket();
                ticket.setIdTicket(1);
                ticket.setIdSolicitante(1);

                Usuario usuario = new Usuario();
                usuario.setIdUsuario(5);
                usuario.setCorreo(correo);
                usuario.setRol(Rol.ESTUDIANTE);

                when(ticketRepository.findById(1))
                                .thenReturn(Optional.of(ticket));

                when(usuarioRepository.findByCorreo(correo))
                                .thenReturn(Optional.of(usuario));

                assertThrows(
                                NoSuchElementException.class,
                                () -> ticketService.buscarPorId(1, correo));
        }

        @Test
        void ticketInexistenteDevuelveError() {

                when(ticketRepository.findById(9999))
                                .thenReturn(Optional.empty());

                assertThrows(
                                NoSuchElementException.class,
                                () -> ticketService.buscarPorId(
                                                9999,
                                                "estudiante.prueba@example.com"));
        }

        @Test
        void noDebePermitirCambioDeEstadoInvalido() {

                String correo = "tecnico.prueba@example.com";

                Ticket ticket = new Ticket();
                ticket.setIdTicket(1);
                ticket.setIdTecnico(2);
                ticket.setEstado(EstadoTicket.EN_PROCESO);

                Usuario tecnico = new Usuario();
                tecnico.setIdUsuario(2);
                tecnico.setCorreo(correo);
                tecnico.setRol(Rol.TECNICO);

                when(ticketRepository.findById(1))
                                .thenReturn(Optional.of(ticket));

                when(usuarioRepository.findByCorreo(correo))
                                .thenReturn(Optional.of(tecnico));

                CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoTicket.CERRADO);

                assertThrows(
                                IllegalStateException.class,
                                () -> ticketService.cambiarEstado(1, request, correo));

                assertEquals(EstadoTicket.EN_PROCESO, ticket.getEstado());

                verify(ticketRepository, never()).save(any(Ticket.class));
        }

        @Test
        void noDebeAsignarTicketAUnEstudiante() {

                // Crear un ticket de prueba
                Ticket ticket = new Ticket();
                ticket.setIdTicket(1);
                ticket.setEstado(EstadoTicket.REGISTRADO);

                // Crear un usuario con rol ESTUDIANTE
                Usuario estudiante = new Usuario();
                estudiante.setIdUsuario(5);
                estudiante.setRol(Rol.ESTUDIANTE);
                estudiante.setEstado(true);

                // Simular las consultas a PostgreSQL
                when(ticketRepository.findById(1))
                                .thenReturn(Optional.of(ticket));

                when(usuarioRepository.findById(5))
                                .thenReturn(Optional.of(estudiante));

                // Intentar asignar el estudiante al ticket
                AsignarTecnicoRequest request = new AsignarTecnicoRequest(5);

                // Debe rechazar la asignacion
                assertThrows(
                                IllegalArgumentException.class,
                                () -> ticketService.asignarTecnico(1, request));

                // Comprobar que no se guardo ninguna modificacion
                assertNull(ticket.getIdTecnico());

                verify(ticketRepository, never())
                                .save(any(Ticket.class));
        }

}
