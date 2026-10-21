# FLUJO DEL PROYECTO - ASM HelpDesk Cibertec

## Índice
1. Problemática Identificada
2. Objetivo del Proyecto
3. Solución Propuesta
4. Arquitectura del Sistema
5. Flujo de Funcionamiento
6. Componentes Principales
7. Tecnologías Utilizadas
8. Beneficios Esperados
9. Conclusión

## 1. Problemática Identificada

En el sistema de Mesa de Ayuda (ASM) del Instituto Cibertec, el asistente virtual chatbot presentaba una deficiencia en la experiencia de usuario (UX). Cuando un usuario finalizaba su consulta con expresiones como "gracias", "eso es todo", "adiós" o "chau", el asistente no reconocía estas palabras de cierre o despedida.

Debido a esta falta de detección, el chatbot caía en el caso por defecto del sistema, volviendo a formular una pregunta de seguimiento ("¿En qué te ayudo?"), generando una conversación inconclusa y poco natural. Esto afectaba la calidad de atención, creaba confusión en el usuario y restaba profesionalismo al servicio automatizado de soporte técnico.

Adicionalmente, existían problemas en la integración con el servicio de IA IBM watsonx.ai, relacionados con la construcción incorrecta de la URL de consumo de la API, lo cual impedía el correcto funcionamiento del asistente inteligente.

## 2. Objetivo del Proyecto

El objetivo principal es mejorar la experiencia de usuario del chatbot del HelpDesk, implementando un cierre adecuado y natural de las conversaciones, evitando preguntas de seguimiento innecesarias cuando el usuario desea finalizar la interacción. Asimismo, corregir la configuración de integración con IBM watsonx.ai para garantizar su correcto funcionamiento.

## 3. Solución Propuesta

Se implementaron dos mejoras puntuales en el servicio backend del chatbot:

### 3.1 Mejora en el asistente local (Heurística de cierre de conversación)

Se agregó una validación inicial en la lógica de respuesta local del chatbot para detectar palabras y frases de agradecimiento o despedida. Cuando se identifica alguna de estas expresiones, el sistema devuelve una respuesta de cierre definitiva, sin incluir ninguna pregunta al final.

Palabras detectadas: "gracias", "adios", "chau", "chao", "listo", "ok", "okay", "perfecto", "genial", "excelente", "resuelto", "solucionado", "bye", "hasta luego", "nos vemos", junto a frases como "eso es todo", "nada mas", "ya esta", "todo listo", "muchas gracias", entre otras.

Respuesta de cierre implementada:
> "¡De nada! Ha sido un placer ayudarte. Si tienes otro problema en el futuro, no dudes en escribirme. ¡Que tienes un excelente día!"

### 3.2 Mejora en el prompt de sistema de IBM watsonx.ai

Se agregó una instrucción estricta al prompt enviado al modelo de IA Generativa, prohibiendo explícitamente realizar preguntas de seguimiento al momento de despedirse o recibir un agradecimiento. Esto asegura consistencia entre el modo local (fallback) y el modo con IA.

Regla agregada:
> "REGLA: Si el usuario te agradece, dice que eso es todo, o se despide, responde únicamente con un mensaje cortés de despedida. ESTÁ ESTRICTAMENTE PROHIBIDO hacer preguntas de seguimiento o pedirle al usuario que te cuente más problemas al despedirte."

### 3.3 Corrección de integración con IBM watsonx.ai

Se corrigió la construcción de la URL para la generación de texto, asegurando el formato correcto para el endpoint WML (Watson Machine Learning):

**Formato corregido:**  
`[WATSONX_URL]/ml/v1/text/generation?version=2024-05-01`

La lógica de normalización garantiza que siempre se incluya el segmento `/ml` antes de `/v1`, independientemente del formato de URL configurado en las variables de entorno.

Además, se corrigió el envío de parámetros: el `project_id` se envía únicamente en el cuerpo JSON de la solicitud, eliminando su envío como parámetro de consulta en la URL (evitando errores HTTP 400).

## 4. Arquitectura del Sistema

El proyecto ASM HelpDesk sigue una arquitectura en capas, con separación entre frontend, backend y servicios externos.

### 4.1 Backend (Spring Boot)
- **Capa de Controlador:** Expone endpoints REST para el chatbot (`ChatbotController`).
- **Capa de Servicio:** Lógica de negocio (`ChatbotServiceImpl`) con soporte dual (IA + fallback local).
- **Cliente Externo:** `WatsonxClient` para integración con IBM watsonx.ai (autenticación IAM y generación de texto).
- **Configuración:** `WatsonxProperties` para manejo de credenciales y parámetros desde variables de entorno.

### 4.2 Frontend (Angular)
- **Componente Chatbot:** Interfaz de usuario para interacción con el asistente virtual.
- **Servicios:** Comunicación HTTP con el backend REST.

### 4.3 Servicios Externos
- **IBM watsonx.ai:** Modelo de IA generativa para respuestas inteligentes.
- **IAM (IBM Cloud Identity and Access Management):** Autenticación mediante API Key para acceso a watsonx.ai.

## 5. Flujo de Funcionamiento

El flujo de conversación del chatbot se ejecuta de la siguiente manera:

1. **Recepción del mensaje:** El usuario envía un mensaje a través de la interfaz web (Angular). Este llega al backend vía endpoint REST.

2. **Validación de configuración:** El sistema verifica si IBM watsonx.ai está configurado (API Key, Project ID, URL, Model ID).

3. **Detección de cierre (Prioritaria):** En modo local (y reforzada en modo IA), se evalúa si el mensaje contiene palabras de agradecimiento o despedida.

   - **Si es cierre:** Se retorna inmediatamente una respuesta de despedida cortés y definitiva, **sin preguntas de seguimiento**. El flujo termina aquí.

4. **Modo con IA (watsonx.ai):** Si no es cierre y está configurado:
   - Se construye el prompt de sistema con las reglas de negocio y la nueva regla de despedida.
   - Se obtiene token IAM (con cache hasta 60 segundos antes de expirar).
   - Se realiza llamada POST a `[WATSONX_URL]/ml/v1/text/generation?version=2024-05-01` con `project_id`, `model_id` y `input` en el cuerpo JSON.
   - Si la respuesta es válida, se devuelve al usuario con origen WATSONX.

5. **Fallback local (Heurístico):** Si no está configurado watsonx.ai, o falla la llamada (error, timeout, respuesta inválida), el sistema cae automáticamente al asistente local basado en reglas.
   - Se normaliza el texto (sin acentos, minúsculas).
   - Se detecta intención: saludo, tickets, estados, accesos, horarios o caso genérico.
   - Se retorna respuesta apropiada.

6. **Respuesta al usuario:** La respuesta (junto a su origen: WATSONX o LOCAL) es enviada de vuelta al frontend y mostrada en la interfaz del chatbot.

## 6. Componentes Principales

### ChatbotServiceImpl.java
Clase principal que gestiona la lógica del chatbot. Contiene:
- Método `respuestaLocalChat()`: Heurística local con detección prioritaria de cierre de conversación.
- Método `construirPromptChat()`: Generación del prompt para watsonx.ai con reglas estrictas de despedida.
- Método `esDespedidaOAgradadecimiento()`: Utilidad para detectar agradecimientos/despedidas con límites de palabra para evitar falsos positivos.
- Métodos auxiliares para clasificación de tickets, normalización de texto y detección de palabras clave.

### WatsonxClient.java
Cliente REST para IBM watsonx.ai. Contiene:
- Autenticación IAM con caching de token.
- Método `generar()`: Invocación a la API de generación de texto.
- Método `urlGeneracion()`: Construcción correcta de URL con `/ml/v1` y parámetro `version` únicamente.
- Manejo robusto de errores (retorna `Optional.empty()` para activar fallback local).

## 7. Tecnologías Utilizadas

- **Backend:** Java 17+, Spring Boot 3.x
- **Frontend:** Angular, TypeScript, HTML, CSS
- **IA:** IBM watsonx.ai (Granite/Modelos LLM disponibles)
- **Autenticación:** IBM Cloud IAM (API Key OAuth2)
- **Base de Datos:** MySQL (para entidades del sistema HelpDesk)
- **Build:** Maven
- **Control de versiones:** Git

## 8. Beneficios Esperados

1. **Mejora de UX:** Las conversaciones con el chatbot finalizan de forma natural y profesional, sin preguntas incómodas tras un agradecimiento o despedida.
2. **Mayor coherencia:** Comportamiento uniforme entre modo IA y modo fallback local.
3. **Reducción de confusión:** El usuario recibe una respuesta clara de cierre, entendiendo que la interacción ha finalizado correctamente.
4. **Mayor robustez:** Integración estable con IBM watsonx.ai gracias a la corrección de la URL y parámetros de la API.
5. **Experiencia más humana:** El asistente se comporta de manera más educada y cercana a una atención humana real.
6. **Disponibilidad continua:** Gracias al sistema de fallback automático (local), el servicio nunca queda inoperativo ante fallos de la API externa.

## 9. Conclusión

Las modificaciones implementadas resuelven eficazmente la problemática de cierre de conversación identificada, mejorando significativamente la experiencia de usuario del asistente virtual ASM HelpDesk. La solución es mínima, puntual y no invasiva, preservando toda la funcionalidad existente del sistema. Con estas mejoras, el chatbot ofrece una interacción más natural, profesional y coherente, alineada con las expectativas de calidad del Instituto Cibertec.
