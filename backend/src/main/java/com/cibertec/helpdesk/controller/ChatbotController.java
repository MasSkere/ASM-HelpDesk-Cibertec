package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.request.ChatRequest;
import com.cibertec.helpdesk.dto.request.SugerirTicketRequest;
import com.cibertec.helpdesk.dto.response.ChatResponse;
import com.cibertec.helpdesk.dto.response.SugerenciaTicketResponse;
import com.cibertec.helpdesk.service.ChatbotService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asistente IA (IBM watsonx.ai) — cualquier usuario autenticado.
 *
 * <ul>
 * <li>{@code POST /api/chatbot/chat}: consulta general en lenguaje natural.</li>
 * <li>{@code POST /api/chatbot/sugerir-ticket}: sugiere tipo, prioridad y
 *     categoría para un ticket nuevo.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request) {

        return ResponseEntity.ok(
                chatbotService.conversar(request));
    }

    @PostMapping("/sugerir-ticket")
    public ResponseEntity<SugerenciaTicketResponse> sugerirTicket(
            @Valid @RequestBody SugerirTicketRequest request) {

        return ResponseEntity.ok(
                chatbotService.sugerirTicket(request));
    }
}
