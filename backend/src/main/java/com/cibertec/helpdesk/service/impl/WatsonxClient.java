package com.cibertec.helpdesk.service.impl;

import com.cibertec.helpdesk.config.WatsonxProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente REST mínimo hacia IBM watsonx.ai (sin SDK externo).
 *
 * <ul>
 * <li>Autenticación IAM: API key → access token (cacheado hasta ~60 s antes
 *     de expirar).</li>
 * <li>Inferencia: {@code POST /v1/text/generation} con modelo y project id.</li>
 * <li>Si algo falla devuelve {@link Optional#empty()}; NUNCA lanza excepción
 *     ni imprime credenciales en los logs.</li>
 * </ul>
 */
@Slf4j
@Component
public class WatsonxClient {

    private static final String IAM_TOKEN_URL =
            "https://iam.cloud.ibm.com/identity/token";
    private static final String WATSONX_VERSION = "2024-05-01";

    private final WatsonxProperties properties;
    private final RestTemplate rest;
    private final ObjectMapper mapper;

    private String tokenIam;
    private Instant tokenExpiraEn;

    public WatsonxClient(
            WatsonxProperties properties,
            ObjectMapper mapper) {

        this.properties = properties;
        this.mapper = mapper;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(
                Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())));

        this.rest = new RestTemplate(factory);
    }

    /**
     * Genera texto con el modelo configurado.
     *
     * @return el texto generado o vacío si IBM no respondió (para que el
     *         llamador caiga al asistente local).
     */
    public Optional<String> generar(String prompt, int maxTokens) {

        try {
            String token = obtenerTokenIam();

            if (token == null) {
                return Optional.empty();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            Map<String, Object> cuerpo = Map.of(
                    "model_id", properties.getModel(),
                    "input", prompt,
                    "project_id", properties.getProjectId(),
                    "parameters", Map.of(
                            "max_new_tokens", maxTokens,
                            "min_new_tokens", 1,
                            "decoding_method", "greedy"));

            HttpEntity<String> entity = new HttpEntity<>(
                    mapper.writeValueAsString(cuerpo), headers);

            ResponseEntity<String> response = rest.postForEntity(
                    urlGeneracion(), entity, String.class);

            return extraerTexto(response.getBody());

        } catch (Exception e) {
            log.warn("watsonx.ai no respondio ({}). Se usara el asistente local.",
                    e.getMessage());
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------
    // Token IAM (API key -> access token, con cache)
    // ------------------------------------------------------------------

    private synchronized String obtenerTokenIam() {

        if (tokenIam != null && tokenExpiraEn != null
                && Instant.now().isBefore(tokenExpiraEn)) {
            return tokenIam;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type",
                    "urn:ibm:params:oauth:grant-type:apikey");
            form.add("apikey", properties.getApikey());

            ResponseEntity<String> response = rest.postForEntity(
                    IAM_TOKEN_URL, new HttpEntity<>(form, headers),
                    String.class);

            JsonNode json = mapper.readTree(response.getBody());

            String accessToken = json.path("access_token").asText(null);

            if (accessToken == null || accessToken.isBlank()) {
                log.warn("IAM no devolvio access_token (revise WATSONX_APIKEY).");
                return null;
            }

            long expiraSegundos = json.path("expires_in").asLong(3600);

            // Se renueva 60 s antes para evitar expirar a media llamada.
            this.tokenIam = accessToken;
            this.tokenExpiraEn = Instant.now()
                    .plusSeconds(Math.max(60, expiraSegundos - 60));

            return tokenIam;

        } catch (Exception e) {
            log.warn("No se pudo obtener el token IAM ({}).", e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
    // URL e interpretación de la respuesta
    // ------------------------------------------------------------------

    /**
     * Normaliza la URL configurada para usar el formato correcto:
     * [WATSONX_URL]/ml/v1/text/generation?version=2024-05-01
     */
    private String urlGeneracion() {

        String base = properties.getUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String endpoint = base;
        if (base.endsWith("/ml/v1")) {
            endpoint = base;
        } else if (base.endsWith("/v1")) {
            endpoint = base.replace("/v1", "/ml/v1");
        } else {
            endpoint = base + "/ml/v1";
        }

        return endpoint + "/text/generation"
                + "?version=" + WATSONX_VERSION;
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private Optional<String> extraerTexto(String body) throws Exception {

        if (body == null || body.isBlank()) {
            return Optional.empty();
        }

        JsonNode raiz = mapper.readTree(body);
        String texto = null;

        JsonNode resultados = raiz.path("results");
        if (resultados.isArray() && resultados.size() > 0) {
            texto = resultados.get(0).path("generated_text").asText(null);
        }
        if (texto == null) {
            texto = raiz.path("generated_text").asText(null);
        }

        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(texto.trim());
    }
}
