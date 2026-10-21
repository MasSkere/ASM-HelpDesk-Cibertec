package com.cibertec.helpdesk.dto.response;

import com.cibertec.helpdesk.entity.Prioridad;
import com.cibertec.helpdesk.entity.TipoSolicitud;

/**
 * Sugerencia de clasificación de ticket generada por el asistente IA.
 *
 * @param tipoSolicitud tipo sugerido
 * @param prioridad     prioridad sugerida
 * @param idCategoria   id de categoría (resuelto contra la BD)
 * @param categoria     nombre de la categoría sugerida
 * @param justificacion breve motivo de la sugerencia (mensaje al usuario)
 * @param origen        "WATSONX" o "LOCAL"
 */
public record SugerenciaTicketResponse(
    TipoSolicitud tipoSolicitud,
    Prioridad prioridad,
    Integer idCategoria,
    String categoria,
    String justificacion,
    String origen
) {
    public static final String ORIGEN_WATSONX = "WATSONX";
    public static final String ORIGEN_LOCAL = "LOCAL";
}
