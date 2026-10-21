package com.cibertec.helpdesk.service.impl;

import com.cibertec.helpdesk.config.WatsonxProperties;
import com.cibertec.helpdesk.dto.request.ChatRequest;
import com.cibertec.helpdesk.dto.request.SugerirTicketRequest;
import com.cibertec.helpdesk.dto.response.ChatResponse;
import com.cibertec.helpdesk.dto.response.SugerenciaTicketResponse;
import com.cibertec.helpdesk.entity.Categoria;
import com.cibertec.helpdesk.entity.Prioridad;
import com.cibertec.helpdesk.entity.TipoSolicitud;
import com.cibertec.helpdesk.repository.CategoriaRepository;
import com.cibertec.helpdesk.service.ChatbotService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Asistente IA: usa IBM watsonx.ai cuando está configurado y, si no hay
 * credenciales o IBM no responde, cae automáticamente a un asistente local
 * basado en reglas (para que la funcionalidad nunca se caiga).
 */
@Slf4j
@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final WatsonxProperties properties;
    private final WatsonxClient watsonxClient;
    private final CategoriaRepository categoriaRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatbotServiceImpl(
            WatsonxProperties properties,
            WatsonxClient watsonxClient,
            CategoriaRepository categoriaRepository) {

        this.properties = properties;
        this.watsonxClient = watsonxClient;
        this.categoriaRepository = categoriaRepository;
    }

    // ------------------------------------------------------------------
    // Chat general
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ChatResponse conversar(ChatRequest request) {

        String mensaje = request.mensaje().trim();

        if (properties.estaConfigurado()) {
            try {
                String prompt = construirPromptChat(mensaje, request.contexto());

                Optional<String> respuesta =
                        watsonxClient.generar(prompt, 400);

                if (respuesta.isPresent()) {
                    return new ChatResponse(
                            respuesta.get(),
                            ChatResponse.ORIGEN_WATSONX);
                }
            } catch (Exception e) {
                log.warn("Fallo watsonx en chat ({}). Fallback local.",
                        e.getMessage());
            }
        }

        return new ChatResponse(
                respuestaLocalChat(mensaje),
                ChatResponse.ORIGEN_LOCAL);
    }

    // ------------------------------------------------------------------
    // Sugerencia de ticket
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SugerenciaTicketResponse sugerirTicket(SugerirTicketRequest request) {

        String asunto = request.asunto().trim();
        String descripcion = request.descripcion().trim();

        List<Categoria> categorias = categoriaRepository.findAll();

        if (categorias.isEmpty()) {
            throw new IllegalStateException(
                    "No hay categorias registradas para clasificar el ticket");
        }

        if (properties.estaConfigurado()) {
            try {
                String prompt = construirPromptSugerencia(
                        asunto, descripcion, categorias);

                Optional<String> respuesta =
                        watsonxClient.generar(prompt, 250);

                if (respuesta.isPresent()) {
                    SugerenciaTicketResponse sugerencia =
                            parsearSugerencia(
                                    respuesta.get(), asunto, descripcion,
                                    categorias);

                    if (sugerencia != null) {
                        return sugerencia;
                    }
                }
            } catch (Exception e) {
                log.warn("Fallo watsonx al sugerir ticket ({}). Fallback local.",
                        e.getMessage());
            }
        }

        return sugerenciaLocal(asunto, descripcion, categorias);
    }

    // ------------------------------------------------------------------
    // Prompts hacia watsonx.ai
    // ------------------------------------------------------------------

    private String construirPromptChat(String mensaje, String contexto) {

        String contextoTexto = "NUEVO_TICKET".equalsIgnoreCase(contexto)
                ? "El usuario esta en la pantalla de creacion de un ticket. "
                : "";

        return """
                Eres el asistente virtual de la Mesa de Ayuda (ASM) del \
                Instituto Cibertec. Ayudas a estudiantes, docentes y tecnicos \
                a reportar y dar seguimiento a incidencias de soporte tecnico \
                (hardware, software, redes, cuentas, correo y aula virtual).

                Reglas:
                - Responde SIEMPRE en español, de forma breve (maximo 4 \
                oraciones) y amable.
                - No inventes funcionalidades que el sistema no tiene. El \
                sistema solo permite: crear tickets, ver su estado \
                (REGISTRADO, EN_PROCESO, RESUELTO, CERRADO, CANCELADO), \
                consultar el historial y ver reportes (solo administradores).
                - No pidas datos personales ni credenciales. Nunca pidas la \
                contraseña del usuario.
                - Si te piden algo fuera de la mesa de ayuda, redirige \
                amablemente al tema de soporte tecnico.
                - Si el usuario describe una incidencia, sugiere crear un \
                ticket con "Nuevo ticket".
                - REGLA: Si el usuario te agradece, dice que eso es todo, o se \
                despide, responde unicamente con un mensaje cortes de \
                despedida. ESTA ESTRICTAMENTE PROHIBIDO hacer preguntas de \
                seguimiento o pedirle al usuario que te cuente mas problemas \
                al despedirte.

                %s
                Pregunta del usuario: %s

                Respuesta:
                """.formatted(contextoTexto, mensaje);
    }

    private String construirPromptSugerencia(
            String asunto,
            String descripcion,
            List<Categoria> categorias) {

        String nombresCategorias = categorias.stream()
                .map(Categoria::getNombre)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        return """
                Clasifica el siguiente ticket de soporte tecnico de la Mesa de \
                Ayuda ASM - Instituto Cibertec.

                Devuelve SOLO un objeto JSON valido (sin markdown, sin texto \
                antes o despues) con EXACTAMENTE estas claves:
                {"tipoSolicitud":"INCIDENTE|REQUERIMIENTO|CONSULTA",\
                "prioridad":"BAJA|MEDIA|ALTA|CRITICA",\
                "categoria":"<nombre exacto de una categoria disponible>",\
                "justificacion":"<frase breve en español de maximo 20 palabras>"}

                Reglas de clasificacion:
                - INCIDENTE: algo dejó de funcionar o funciona mal.
                - REQUERIMIENTO: se pide instalar, habilitar o dotar un \
                equipo/software.
                - CONSULTA: es solo una duda o solicitud de información.
                - CRITICA: todo un area o laboratorio sin servicio, o urgencia \
                extrema.
                - ALTA: impide trabajar a una persona o grupo, sin conexión, \
                no enciende.
                - MEDIA: molesta pero tiene alternativa.
                - BAJA: duda, mejora menor o sin apuro.
                - categoria: usa SOLO un nombre exacto de la lista disponible.

                Categorias disponibles: %s

                Asunto: %s
                Descripcion: %s

                JSON:
                """.formatted(nombresCategorias, asunto, descripcion);
    }

    // ------------------------------------------------------------------
    // Parseo de la respuesta de watsonx (JSON)
    // ------------------------------------------------------------------

    /**
     * Convierte el JSON devuelto por el modelo en una sugerencia válida.
     * Si el modelo respondió algo inesperado devuelve {@code null} para que
     * el llamador use el fallback local.
     */
    private SugerenciaTicketResponse parsearSugerencia(
            String texto,
            String asunto,
            String descripcion,
            List<Categoria> categorias) {

        try {
            String json = extraerJson(texto);
            JsonNode nodo = mapper.readTree(json);

            TipoSolicitud tipo = parsearTipo(nodo.path("tipoSolicitud").asText(""));
            Prioridad prioridad = parsearPrioridad(nodo.path("prioridad").asText(""));
            Categoria categoria = resolverCategoria(
                    nodo.path("categoria").asText(""), categorias);

            String justificacion = nodo.path("justificacion").asText("").trim();

            if (justificacion.isBlank()) {
                justificacion = "Clasificacion sugerida por el asistente IA.";
            }

            return new SugerenciaTicketResponse(
                    tipo,
                    prioridad,
                    categoria.getIdCategoria(),
                    categoria.getNombre(),
                    justificacion,
                    SugerenciaTicketResponse.ORIGEN_WATSONX);

        } catch (Exception e) {
            log.debug("watsonx devolvio JSON inutilizable para la sugerencia.");
            return null;
        }
    }

    /** Elimina vallas de código y texto alrededor para aislar el JSON. */
    private String extraerJson(String texto) {

        String limpio = texto.trim();

        int inicio = limpio.indexOf('{');
        int fin = limpio.lastIndexOf('}');

        if (inicio >= 0 && fin > inicio) {
            limpio = limpio.substring(inicio, fin + 1);
        }
        return limpio;
    }

    private TipoSolicitud parsearTipo(String valor) {
        try {
            return TipoSolicitud.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoSolicitud.INCIDENTE;
        }
    }

    private Prioridad parsearPrioridad(String valor) {
        try {
            return Prioridad.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Prioridad.MEDIA;
        }
    }

    /** Busca la categoría por nombre exacto; si no, por coincidencia parcial. */
    private Categoria resolverCategoria(
            String nombre,
            List<Categoria> categorias) {

        String normalizado = normalizar(nombre);

        Optional<Categoria> exacta = categorias.stream()
                .filter(c -> normalizar(c.getNombre()).equals(normalizado))
                .findFirst();

        if (exacta.isPresent()) {
            return exacta.get();
        }

        return categorias.stream()
                .filter(c -> normalizar(c.getNombre()).contains(normalizado)
                        || normalizado.contains(normalizar(c.getNombre())))
                .findFirst()
                .orElse(categorias.get(0));
    }

    // ------------------------------------------------------------------
    // Asistente local (fallback heurístico, sin IA externa)
    // ------------------------------------------------------------------

    private SugerenciaTicketResponse sugerenciaLocal(
            String asunto,
            String descripcion,
            List<Categoria> categorias) {

        String texto = normalizar(asunto + " " + descripcion);

        Categoria categoria = categoriaPorPalabrasClave(texto, categorias);
        Prioridad prioridad = prioridadPorPalabrasClave(texto);
        TipoSolicitud tipo = tipoPorPalabrasClave(texto);

        String justificacion = "Clasificado por el asistente local: "
                + "categoria '" + categoria.getNombre()
                + "', prioridad " + prioridad.name()
                + " y tipo " + tipo.name() + ".";
        justificacion = justificacion.toLowerCase();

        return new SugerenciaTicketResponse(
                tipo,
                prioridad,
                categoria.getIdCategoria(),
                categoria.getNombre(),
                justificacion,
                SugerenciaTicketResponse.ORIGEN_LOCAL);
    }

    private Categoria categoriaPorPalabrasClave(
            String texto,
            List<Categoria> categorias) {

        return categorias.stream()
                .filter(c -> palabrasClaveDe(c.getNombre()).stream()
                        .anyMatch(texto::contains))
                .findFirst()
                .orElse(categorias.get(0));
    }

    /** Palabras clave (ya normalizadas) asociadas a cada categoría semilla. */
    private List<String> palabrasClaveDe(String nombreCategoria) {

        String nombre = normalizar(nombreCategoria);

        if (nombre.contains("redes")) {
            return List.of("wifi", "wi-fi", "internet", "red", "conexion",
                    "cable", "router", "senal", "lan", "dns", "vpn");
        }
        if (nombre.contains("cuentas") || nombre.contains("accesos")) {
            return List.of("cuenta", "password", "clave", "contrasena",
                    "acceso", "login", "ingresar", "credenciales",
                    "bloqueada", "bloqueo", "restablecer");
        }
        if (nombre.contains("correo")) {
            return List.of("correo", "email", "outlook", "buzon",
                    "mensaje", "envio", "recepcion");
        }
        if (nombre.contains("aula") || nombre.contains("virtual")) {
            return List.of("aula virtual", "lms", "plataforma", "canvas",
                    "moodle", "tarea", "matricula", "nota", "clase virtual");
        }
        if (nombre.contains("hardware")) {
            return List.of("pc", "computadora", "laptop", "impresora",
                    "mouse", "teclado", "pantalla", "monitor", "no enciende",
                    "bateria", "cable", "periferico");
        }
        if (nombre.contains("software")) {
            return List.of("instalar", "instalacion", "programa",
                    "aplicacion", "software", "windows", "office", "error",
                    "licencia");
        }
        return List.of();
    }

    private Prioridad prioridadPorPalabrasClave(String texto) {

        if (contiene(texto, "urgente", "critico", "paralizado", "todo el laboratorio",
                "no puedo trabajar", "sin servicio total", "emergencia")) {
            return Prioridad.CRITICA;
        }
        if (contiene(texto, "no funciona", "no enciende", "sin conexion",
                "no hay conexion", "no conecta", "sin internet",
                "no hay internet", "no tiene internet", "caido", "bloqueado",
                "no puedo", "error", "lent", "no recibe", "no envia")) {
            return Prioridad.ALTA;
        }
        if (contiene(texto, "duda", "consulta", "como", "donde", "cuando",
                "informacion", "quiero saber", "pregunta")) {
            return Prioridad.BAJA;
        }
        return Prioridad.MEDIA;
    }

    private TipoSolicitud tipoPorPalabrasClave(String texto) {

        if (contiene(texto, "instalar", "instalacion", "solicito", "solicitud",
                "necesito", "requiero", "dotar", "proveer", "habilitar",
                "actualizar")) {
            return TipoSolicitud.REQUERIMIENTO;
        }
        if (contiene(texto, "duda", "consulta", "como", "donde", "cuanto",
                "cuando", "informacion", "pregunta", "quiero saber")) {
            return TipoSolicitud.CONSULTA;
        }
        return TipoSolicitud.INCIDENTE;
    }

    private String respuestaLocalChat(String mensaje) {

        String texto = normalizar(mensaje);

        // Despedida / agradecimiento: cierre definitivo, SIN preguntas de seguimiento.
        // Se evalua primero para que "gracias" no caiga en el saludo ni en el caso por defecto.
        if (esDespedidaOAgradadecimiento(texto)) {
            return "¡De nada! Ha sido un placer ayudarte. Si tienes otro "
                    + "problema en el futuro, no dudes en escribirme. "
                    + "¡Que tengas un excelente día!";
        }

        if (contiene(texto, "hola", "buenos dias", "buenas tardes", "buenas noches",
                "que tal", "hey")) {
            return "¡Hola! Soy el asistente de la Mesa de Ayuda ASM de "
                    + "Cibertec. Puedo orientarte para reportar una incidencia, "
                    + "saber cómo crear un ticket o consultar sus estados. "
                    + "¿En qué te ayudo?";
        }
        if (contiene(texto, "ticket", "incidencia", "reportar", "problema",
                "falla", "no funciona", "error")) {
            return "Para reportarlo, ve a \"Nuevo ticket\", describe el asunto "
                    + "y la descripción del problema, elige tipo, prioridad y "
                    + "categoría (puedes usar \"Sugerir con IA\" para que te "
                    + "ayude a clasificarlo) y presiona \"Registrar ticket\". "
                    + "Luego podrás ver su estado en tu bandeja.";
        }
        if (contiene(texto, "estado", "seguimiento", "avance", "actualizar")) {
            return "Puedes ver el estado de tus tickets en \"Mis tickets\": "
                    + "REGISTRADO (recién creado), EN_PROCESO (lo atiende un "
                    + "técnico), RESUELTO, CERRADO o CANCELADO. Cada cambio "
                    + "queda en el historial del ticket.";
        }
        if (contiene(texto, "contrasena", "clave", "password", "cuenta",
                "acceso", "ingresar")) {
            return "Si no puedes ingresar, usa \"¿Olvidaste tu contraseña?\" "
                    + "en la pantalla de inicio de sesión. Si el problema "
                    + "continúa, crea un ticket de la categoría "
                    + "\"Cuentas y Accesos\" y un técnico te ayudará. "
                    + "Nunca compartas tu contraseña.";
        }
        if (contiene(texto, "horario", "atienden", "soporte", "contacto",
                "telefono")) {
            return "La Mesa de Ayuda atiende por medio de tickets dentro del "
                    + "sistema. Registra tu solicitud con el mayor detalle "
                    + "posible y un técnico la asignará pronto.";
        }

        return "Puedo ayudarte a crear tickets de soporte (hardware, software, "
                + "redes, cuentas, correo o aula virtual), explicarte los "
                + "estados de un ticket o cómo seguirlo. Cuéntame qué "
                + "necesitas reportar.";
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /**
     * Detecta agradecimientos o despedidas que deben cerrar la conversación.
     *
     * <p>Se evalúa sobre el texto normalizado (minúsculas y sin acentos) y con
     * límites de palabra (\b) para no confundir, por ejemplo, "listo" con
     * "listado". Nótese que "gracias" también cubre "muchas gracias".</p>
     */
    private boolean esDespedidaOAgradadecimiento(String texto) {

        if (texto.isBlank()) {
            return false;
        }

        // Frases de cierre (contienen varias palabras): coincidencia directa.
        if (contiene(texto, "eso es todo", "nada mas", "nada más",
                "es todo", "era todo", "ya esta", "ya está", "todo listo",
                "muchas gracias", "mil gracias")) {
            return true;
        }

        // Palabras sueltas de cierre: con límite de palabra para evitar
        // falsos positivos dentro de otras palabras ("listo" ≠ "listado").
        return Pattern.compile(
                "\\b(gracias|adios|chau|chao|listo|ok|okay|perfecto|"
                        + "genial|excelente|resuelto|solucionado|bye|"
                        + "hasta luego|nos vemos)\\b")
                .matcher(texto)
                .find();
    }

    private boolean contiene(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    /** Minúsculas y sin acentos, para comparar palabras clave. */
    private String normalizar(String valor) {
        String sinAcentos = Normalizer.normalize(
                valor.toLowerCase(), Normalizer.Form.NFD);
        return sinAcentos.replaceAll("\\p{M}", "");
    }
}
