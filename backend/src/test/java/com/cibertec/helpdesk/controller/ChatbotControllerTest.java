package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.response.ChatResponse;
import com.cibertec.helpdesk.dto.response.SugerenciaTicketResponse;
import com.cibertec.helpdesk.entity.Prioridad;
import com.cibertec.helpdesk.entity.TipoSolicitud;
import com.cibertec.helpdesk.exception.GlobalExceptionHandler;
import com.cibertec.helpdesk.service.ChatbotService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatbotControllerTest {

        @Mock
        private ChatbotService chatbotService;

        @InjectMocks
        private ChatbotController chatbotController;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .standaloneSetup(chatbotController)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void chatDevuelve200ConRespuestaEstandar() throws Exception {

                when(chatbotService.conversar(any())).thenReturn(
                                new ChatResponse("¡Hola! Puedo ayudarte.",
                                                ChatResponse.ORIGEN_LOCAL));

                mockMvc.perform(post("/api/chatbot/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"mensaje\":\"Hola\",\"contexto\":\"GENERAL\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.respuesta").value("¡Hola! Puedo ayudarte."))
                                .andExpect(jsonPath("$.origen").value("LOCAL"));
        }

        @Test
        void chatSinMensajeDevuelve400() throws Exception {

                mockMvc.perform(post("/api/chatbot/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"mensaje\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void sugerirTicketDevuelve200ConSugerencia() throws Exception {

                when(chatbotService.sugerirTicket(any())).thenReturn(
                                new SugerenciaTicketResponse(
                                                TipoSolicitud.INCIDENTE,
                                                Prioridad.ALTA,
                                                3,
                                                "Redes e Internet",
                                                "Sin conexión en laboratorio",
                                                SugerenciaTicketResponse.ORIGEN_WATSONX));

                mockMvc.perform(post("/api/chatbot/sugerir-ticket")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"asunto\":\"No hay wifi\",\"descripcion\":\"No conecta\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tipoSolicitud").value("INCIDENTE"))
                                .andExpect(jsonPath("$.prioridad").value("ALTA"))
                                .andExpect(jsonPath("$.idCategoria").value(3))
                                .andExpect(jsonPath("$.categoria").value("Redes e Internet"))
                                .andExpect(jsonPath("$.origen").value("WATSONX"));
        }

        @Test
        void sugerirTicketSinAsuntoDevuelve400() throws Exception {

                mockMvc.perform(post("/api/chatbot/sugerir-ticket")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"asunto\":\"\",\"descripcion\":\"algo\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }
}
