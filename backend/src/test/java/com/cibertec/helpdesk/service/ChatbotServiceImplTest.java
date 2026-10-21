package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.config.WatsonxProperties;
import com.cibertec.helpdesk.dto.request.ChatRequest;
import com.cibertec.helpdesk.dto.request.SugerirTicketRequest;
import com.cibertec.helpdesk.dto.response.ChatResponse;
import com.cibertec.helpdesk.dto.response.SugerenciaTicketResponse;
import com.cibertec.helpdesk.entity.Categoria;
import com.cibertec.helpdesk.entity.Prioridad;
import com.cibertec.helpdesk.entity.TipoSolicitud;
import com.cibertec.helpdesk.repository.CategoriaRepository;
import com.cibertec.helpdesk.service.impl.ChatbotServiceImpl;
import com.cibertec.helpdesk.service.impl.WatsonxClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceImplTest {

        @Mock
        private WatsonxClient watsonxClient;

        @Mock
        private CategoriaRepository categoriaRepository;

        private WatsonxProperties properties;
        private ChatbotService chatbotService;

        private static final List<Categoria> CATEGORIAS = List.of(
                        Categoria.builder().idCategoria(1).nombre("Hardware").build(),
                        Categoria.builder().idCategoria(2).nombre("Software").build(),
                        Categoria.builder().idCategoria(3).nombre("Redes e Internet").build(),
                        Categoria.builder().idCategoria(4).nombre("Cuentas y Accesos").build());

        @BeforeEach
        void setUp() {
                properties = new WatsonxProperties(); // sin credenciales -> modo local
                chatbotService = new ChatbotServiceImpl(
                                properties, watsonxClient, categoriaRepository);
        }

        // ------------------------------------------------------------------
        // Chat
        // ------------------------------------------------------------------

        @Test
        void chatUsaAsistenteLocalCuandoNoHayCredenciales() {

                ChatResponse respuesta = chatbotService.conversar(
                                new ChatRequest("Hola, tengo un problema con el wifi", null));

                assertNotNull(respuesta.respuesta());
                assertFalse(respuesta.respuesta().isBlank());
                assertEquals(ChatResponse.ORIGEN_LOCAL, respuesta.origen());

                verifyNoInteractions(watsonxClient);
        }

        @Test
        void chatUsaWatsonxCuandoEstaConfigurado() {

                properties.setEnabled(true);
                properties.setApikey("fake-key");
                properties.setProjectId("fake-project");

                when(watsonxClient.generar(anyString(), anyInt()))
                                .thenReturn(Optional.of("Hola, puedo ayudarte."));

                ChatResponse respuesta = chatbotService.conversar(
                                new ChatRequest("Hola", "GENERAL"));

                assertEquals("Hola, puedo ayudarte.", respuesta.respuesta());
                assertEquals(ChatResponse.ORIGEN_WATSONX, respuesta.origen());
        }

        @Test
        void chatCaeALocalSiWatsonxFalla() {

                properties.setEnabled(true);
                properties.setApikey("fake-key");
                properties.setProjectId("fake-project");

                when(watsonxClient.generar(anyString(), anyInt()))
                                .thenReturn(Optional.empty());

                ChatResponse respuesta = chatbotService.conversar(
                                new ChatRequest("Hola", null));

                assertEquals(ChatResponse.ORIGEN_LOCAL, respuesta.origen());
                assertFalse(respuesta.respuesta().isBlank());
        }

        @Test
        void despedidaLocalCierraSinPreguntasDeSeguimiento() {

                ChatResponse respuesta = chatbotService.conversar(
                                new ChatRequest("Muchas gracias, eso es todo", null));

                assertEquals(ChatResponse.ORIGEN_LOCAL, respuesta.origen());
                assertTrue(respuesta.respuesta().contains("De nada"));
                // Debe ser un cierre definitivo: sin preguntas.
                assertFalse(respuesta.respuesta().contains("?"));
        }

        @Test
        void despedidaReconoceVariantes() {

                for (String mensaje : List.of("gracias", "adiós", "chau", "listo",
                                "nada mas", "eso es todo", "nos vemos", "ok")) {
                        ChatResponse respuesta = chatbotService.conversar(
                                        new ChatRequest(mensaje, "GENERAL"));

                        assertTrue(respuesta.respuesta().contains("De nada"),
                                        "Deberia despedirse con: " + mensaje);
                        assertFalse(respuesta.respuesta().contains("?"),
                                        "No debe preguntar con: " + mensaje);
                }
        }

        @Test
        void consultaNormalNoSeConfundeConDespedida() {

                // "listado" contiene "list" pero NO debe disparar la despedida.
                ChatResponse respuesta = chatbotService.conversar(
                                new ChatRequest("Necesito el listado de laboratorios", null));

                assertFalse(respuesta.respuesta().contains("De nada"));
        }

        // ------------------------------------------------------------------
        // Sugerencia de ticket
        // ------------------------------------------------------------------

        @Test
        void sugerenciaLocalClasificaIncidenteDeRed() {

                when(categoriaRepository.findAll()).thenReturn(CATEGORIAS);

                SugerenciaTicketResponse sugerencia = chatbotService.sugerirTicket(
                                new SugerirTicketRequest(
                                                "No hay conexión Wi-Fi en el laboratorio 3",
                                                "Desde ayer no conecta a internet ningún equipo del laboratorio"));

                assertEquals(TipoSolicitud.INCIDENTE, sugerencia.tipoSolicitud());
                assertEquals(Prioridad.ALTA, sugerencia.prioridad());
                assertEquals(3, sugerencia.idCategoria());
                assertEquals("Redes e Internet", sugerencia.categoria());
                assertEquals(SugerenciaTicketResponse.ORIGEN_LOCAL, sugerencia.origen());

                verifyNoInteractions(watsonxClient);
        }

        @Test
        void sugerenciaLocalClasificaDudaComoConsultaBaja() {

                when(categoriaRepository.findAll()).thenReturn(CATEGORIAS);

                SugerenciaTicketResponse sugerencia = chatbotService.sugerirTicket(
                                new SugerirTicketRequest(
                                                "Consulta sobre matrícula",
                                                "Tengo una duda, ¿dónde veo mis notas en el aula virtual?"));

                assertEquals(TipoSolicitud.CONSULTA, sugerencia.tipoSolicitud());
                assertEquals(Prioridad.BAJA, sugerencia.prioridad());
                assertEquals(SugerenciaTicketResponse.ORIGEN_LOCAL, sugerencia.origen());
        }

        @Test
        void sugerenciaLocalSinCoinCIDenciasUsaPrimeraCategoria() {

                when(categoriaRepository.findAll()).thenReturn(CATEGORIAS);

                SugerenciaTicketResponse sugerencia = chatbotService.sugerirTicket(
                                new SugerirTicketRequest("Asunto raro", "Descripción sin palabras clave"));

                assertEquals(CATEGORIAS.get(0).getIdCategoria(), sugerencia.idCategoria());
                assertNotNull(sugerencia.justificacion());
        }

        @Test
        void sugerenciaUsaWatsonxCuandoDevuelveJsonValido() {

                properties.setEnabled(true);
                properties.setApikey("fake-key");
                properties.setProjectId("fake-project");

                when(categoriaRepository.findAll()).thenReturn(CATEGORIAS);
                when(watsonxClient.generar(anyString(), anyInt())).thenReturn(Optional.of(
                                "{\"tipoSolicitud\":\"REQUERIMIENTO\",\"prioridad\":\"BAJA\","
                                                + "\"categoria\":\"Software\",\"justificacion\":\""
                                                + "Solicita instalación de programa\"}"));

                SugerenciaTicketResponse sugerencia = chatbotService.sugerirTicket(
                                new SugerirTicketRequest(
                                                "Instalar Visual Studio",
                                                "Necesito instalar el programa en mi laptop"));

                assertEquals(TipoSolicitud.REQUERIMIENTO, sugerencia.tipoSolicitud());
                assertEquals(Prioridad.BAJA, sugerencia.prioridad());
                assertEquals(2, sugerencia.idCategoria());
                assertEquals(SugerenciaTicketResponse.ORIGEN_WATSONX, sugerencia.origen());
        }

        @Test
        void sugerenciaCaeALocalSiWatsonxDevuelveJsonInvalido() {

                properties.setEnabled(true);
                properties.setApikey("fake-key");
                properties.setProjectId("fake-project");

                when(categoriaRepository.findAll()).thenReturn(CATEGORIAS);
                when(watsonxClient.generar(anyString(), anyInt()))
                                .thenReturn(Optional.of("no soy un json"));

                SugerenciaTicketResponse sugerencia = chatbotService.sugerirTicket(
                                new SugerirTicketRequest(
                                                "No enciende la PC",
                                                "La computadora del aula 2 no da señales de vida"));

                assertEquals(SugerenciaTicketResponse.ORIGEN_LOCAL, sugerencia.origen());
                assertEquals(TipoSolicitud.INCIDENTE, sugerencia.tipoSolicitud());
        }

        @Test
        void sugerenciaSinCategoriasLanzaError() {

                when(categoriaRepository.findAll()).thenReturn(List.of());

                IllegalStateException excepcion = assertThrows(
                                IllegalStateException.class,
                                () -> chatbotService.sugerirTicket(
                                                new SugerirTicketRequest("Asunto", "Descripción")));

                assertTrue(excepcion.getMessage().contains("categorias"));
        }

        // Las prioridades del fallback local son deterministas por palabras clave:
        @Test
        void urgenciaExtremaSeClasificaComoCritica() {

                when(categoriaRepository.findAll()).thenReturn(CATEGORIAS);

                SugerenciaTicketResponse sugerencia = chatbotService.sugerirTicket(
                                new SugerirTicketRequest(
                                                "URGENTE: laboratorio 1 sin servicio",
                                                "Todo el laboratorio está paralizado, es una emergencia"));

                assertEquals(Prioridad.CRITICA, sugerencia.prioridad());
        }
}
