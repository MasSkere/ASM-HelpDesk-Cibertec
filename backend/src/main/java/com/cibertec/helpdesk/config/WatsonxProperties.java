package com.cibertec.helpdesk.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades del asistente IA — IBM watsonx.ai.
 *
 * <p>Los valores SIEMPRE se cargan de variables de entorno (ver {@code .env};
 * nunca se escriben credenciales en el código ni en el repositorio).</p>
 *
 * <p>El asistente se activa solo si {@code enabled=true} y existen
 * {@code apikey} + {@code projectId}; si faltan, el sistema usa
 * automáticamente el asistente local (heurístico) como respaldo.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ibm.watsonx")
public class WatsonxProperties {

    /** Habilita el envío de consultas a IBM watsonx.ai. */
    private boolean enabled = false;

    /** API key de IBM Cloud (WATSONX_APIKEY). */
    private String apikey = "";

    /** Endpoint regional, ej.: https://api.us-south.watsonx.ai.api.cloud.ibm.com */
    private String url = "https://api.us-south.watsonx.ai.api.cloud.ibm.com";

    /** ID del proyecto de watsonx.ai (WATSONX_PROJECT_ID). */
    private String projectId = "";

    /** Modelo a usar, ej.: ibm/granite-3-8b-instruct */
    private String model = "ibm/granite-3-8b-instruct";

    /** Timeout de lectura hacia IBM en segundos. */
    private int timeoutSeconds = 60;

    /** Indica si hay credenciales suficientes para llamar a IBM. */
    public boolean estaConfigurado() {
        return enabled
                && apikey != null && !apikey.isBlank()
                && projectId != null && !projectId.isBlank()
                && url != null && !url.isBlank();
    }
}
