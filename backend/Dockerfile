# =============================================================================
#  Dockerfile multi-stage — Backend ASM-HelpDesk-Cibertec (Monolito Spring Boot)
# -----------------------------------------------------------------------------
#  Contexto de build: ./backend   (donde vive el pom.xml)
#  Uso:
#    docker build -t asm-helpdesk-backend ./backend
# =============================================================================

# ------------------------- Etapa 1: BUILD con Maven --------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1) Copiar solo el pom y descargar dependencias (capa cacheable)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 2) Copiar el código y compilar (sin tests para acelerar la imagen)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------------------- Etapa 2: RUNTIME ligero (JRE) ------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario no root (seguridad)
RUN addgroup -S app && adduser -S app -G app

# Copiar únicamente el JAR generado
COPY --from=build /app/target/*.jar app.jar

USER app
EXPOSE 8080

# Variables de JVM (ajustables por entorno)
ENV JAVA_OPTS=""

# Healthcheck usando Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
