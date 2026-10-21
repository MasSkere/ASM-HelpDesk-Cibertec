package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.request.ChatRequest;
import com.cibertec.helpdesk.dto.request.SugerirTicketRequest;
import com.cibertec.helpdesk.dto.response.ChatResponse;
import com.cibertec.helpdesk.dto.response.SugerenciaTicketResponse;

/**
 * Asistente IA de la Mesa de Ayuda.
 *
 * <p>Responde consultas generales del usuario y sugiere la clasificación
 * (tipo, prioridad y categoría) de un ticket antes de registrarlo.</p>
 *
 * <p>Implementación: IBM watsonx.ai con respaldo local (heurístico) cuando
 * no hay credenciales o el servicio externo no responde.</p>
 */
public interface ChatbotService {

    /** Responde una consulta en lenguaje natural del usuario autenticado. */
    ChatResponse conversar(ChatRequest request);

    /** Sugiere tipo, prioridad y categoría a partir de un asunto + descripción. */
    SugerenciaTicketResponse sugerirTicket(SugerirTicketRequest request);
}
