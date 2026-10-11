# INSTRUCCIONES_AGENTE.md — Reglas de Desarrollo del Backend

> **Este documento es un "System Prompt" / manual obligatorio para cualquier agente de IA
> o desarrollador que programe el backend de `ASM-HelpDesk-Cibertec`.**
> Su incumplimiento es motivo de rechazo del Pull Request.

- **Proyecto:** ASM-HelpDesk-Cibertec (Mesa de Ayuda y Soporte Técnico con IA)
- **Stack backend:** Java 17+ · Spring Boot 3.x · Spring Data JPA · PostgreSQL 15+
- **Arquitectura:** Monolito en capas
- **Package base:** `com.cibertec.helpdesk`

---

## 0. Principios NO negociables

1. **Arquitectura monolítica.** Todo el backend vive en **un único proyecto** y se
   despliega como **un único artefacto** (`backend`). Prohibido crear microservicios,
   módulos separados desplegables o repositorios múltiples.
2. **Separación estricta de capas.** `Controller → Service → Repository → Entity`.
   Nunca se salta una capa (un Controller **no** llama a un Repository directamente).
3. **Prohibido exponer `Entity` en los Controllers.** La API pública se define con **DTOs**
   (`Request`/`Response`) y la conversión se hace con un **Mapper**.
4. **Spring Security obligatorio** en todas las rutas (salvo login/registro/health).
5. **Errores estandarizados** vía `@ControllerAdvice`, nunca `try/catch` devolviendo `Map`.
6. **Todo pasa por Docker.** El backend debe poderse levantar con `docker compose up`.

---

## 1. Arquitectura Monolítica

- **Un solo proyecto** `backend/` (Spring Boot). Todas las funcionalidades se implementan
  como paquetes/módulos lógicos dentro del mismo artefacto.
- Se permite organizar por **features** o por **capas técnicas** — este proyecto usa
  **por capas** (ver sección 2).
- La comunicación entre módulos es por **llamadas a métodos de servicio** (in-process),
  nunca HTTP interno entre ellos.
- ❌ No crear un `gateway`, `eureka`, `config-server`, ni múltiples `bootRun`.

---

## 2. Patrón de Capas (obligatorio)

Ruta base: `src/main/java/com/cibertec/helpdesk/`

| Capa                    | Responsabilidad                                                        | Anotaciones                                    |
| ----------------------- | ---------------------------------------------------------------------- | ---------------------------------------------- |
| `controller/`           | Exponer endpoints REST, validar entrada, **devolver DTOs**.            | `@RestController`, `@RequestMapping`           |
| `service/` + `impl/`    | Lógica de negocio, transacciones, orquestación.                        | `@Service`, `@Transactional`                   |
| `repository/`           | Acceso a datos.                                                        | `@Repository` o interfaz `JpaRepository`       |
| `entity/`               | Mapeo JPA de tablas. **Nunca sale de las capas internas.**             | `@Entity`, `@Table`, `@Id`                     |
| `dto/request/`          | Datos que entran (`record`/clase).                                     | Bean Validation (`@NotNull`, `@Email`...)      |
| `dto/response/`         | Datos que salen.                                                       | —                                              |
| `mapper/`               | Conversión `Entity ⇄ DTO`.                                             | MapStruct (`@Mapper`) o clase `@Component`     |
| `security/`             | Configuración de seguridad.                                            | Ver sección 5                                  |
| `exception/`            | Manejo global de errores.                                              | `@RestControllerAdvice`                        |

### Reglas por capa

- **Controller:** sin lógica de negocio, sin acceso a `Entity`, sin `Repository`.
  Retorna `ResponseEntity<XxxResponse>` con el código HTTP correcto (201, 200, 204...).
- **Service:** define **interfaz** e **implementación** (`XxxService` / `XxxServiceImpl`).
  Anotar métodos de escritura con `@Transactional`.
- **Repository:** interfaces que extienden `JpaRepository<Entity, ID>`. Consultas
  personalizadas con `@Query`/derived queries. **Prohibido SQL nativo** salvo justificación.
- **Entity:** representa la tabla. Relaciones con `@ManyToOne`/`@OneToMany`. Usar
  `FetchType.LAZY` por defecto.

---

## 3. DTOs y Mappers (prohibido exponer Entities)

- **Toda** petición y respuesta del API usa DTOs.
- Nomenclatura:
  - Entrada: `XxxRequest` (ej. `TicketRequest`, `LoginRequest`).
  - Salida: `XxxResponse` (ej. `TicketResponse`).
- Validación en los `Request` con Jakarta Bean Validation y `@Valid` en el controller.
- Conversión con **Mapper** (recomendado **MapStruct**; se acepta mapeo manual en una
  clase `@Component`). El mapper **nunca** contiene lógica de negocio.

### Ejemplos de referencia

```java
// dto/request/TicketRequest.java
public record TicketRequest(
        @NotBlank @Size(max = 150) String asunto,
        String descripcion,
        @NotNull TipoSolicitud tipoSolicitud,
        @NotNull Prioridad prioridad,
        @NotNull Long idCategoria
) {}
```

```java
// mapper/TicketMapper.java  (MapStruct)
@Mapper(componentModel = "spring")
public interface TicketMapper {
    TicketResponse toResponse(Ticket entity);
    Ticket toEntity(TicketRequest request);
    List<TicketResponse> toResponseList(List<Ticket> entities);
}
```

```java
// controller/TicketController.java
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.crear(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.obtenerPorId(id));
    }
}
```

---

## 4. Modelo de datos (referencia)

Tablas definidas en `database/schema.sql`: `usuarios`, `categorias`, `tickets`.
Las `Entity` deben reflejar exactamente columnas, tipos y relaciones de ese script.

- Roles válidos: `ESTUDIANTE`, `DOCENTE`, `TECNICO`, `ADMINISTRADOR`.
- Estado de ticket: `REGISTRADO`, `EN_PROCESO`, `RESUELTO`, `CERRADO`, `CANCELADO`.
- Prioridad: `BAJA`, `MEDIA`, `ALTA`, `CRITICA`.
- Tipo de solicitud: `INCIDENTE`, `REQUERIMIENTO`, `CONSULTA`.

---

## 5. Spring Security (obligatorio)

- **Autenticación por JWT** (stateless, sin sesiones).
- Contraseñas cifradas con **BCrypt** (`PasswordEncoder`).
- Filtro propio `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.
- Autorización por roles con `@PreAuthorize` o `requestMatchers(...).hasRole(...)`.

### Matriz de acceso sugerida

| Ruta                          | Método          | Roles permitidos                        |
| ----------------------------- | --------------- | --------------------------------------- |
| `/api/auth/login`             | POST            | público                                 |
| `/api/auth/register`          | POST            | público (por defecto `ESTUDIANTE`)      |
| `/api/tickets`                | POST            | `ESTUDIANTE`, `DOCENTE`                 |
| `/api/tickets/mios`           | GET             | `ESTUDIANTE`, `DOCENTE`                 |
| `/api/tickets/asignados`      | GET             | `TECNICO`                               |
| `/api/tickets/**` (gestión)   | PUT/PATCH       | `TECNICO`, `ADMINISTRADOR`              |
| `/api/usuarios/**`            | *               | `ADMINISTRADOR`                         |
| `/api/categorias`             | GET             | autenticado                             |
| `/api/categorias/**`          | POST/PUT/DELETE | `ADMINISTRADOR`                         |

> Los roles en Spring Security se almacenan con prefijo `ROLE_`
> (ej. `ROLE_TECNICO`) y se verifican con `hasRole("TECNICO")`.

### Ejemplo de configuración base

```java
// security/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/usuarios/**", "/api/categorias/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/tickets/asignados/**").hasRole("TECNICO")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

```java
// security/jwt/JwtService.java (resumen)
// - generateToken(UserDetails) -> firma HS256 con secreto de entorno (JWT_SECRET)
// - extractUsername(token), isTokenValid(token, userDetails)
// - claims: sub (correo), rol, exp
```

**Reglas de seguridad:**

- El secreto JWT **nunca** se hardcodea: se lee de `application.yml` como
  `${JWT_SECRET}` (variable de entorno).
- El endpoint `/api/auth/login` devuelve `{ "token": "...", "tipo": "Bearer", ... }`.
- No loggear contraseñas ni tokens.

---

## 6. Manejo de Excepciones (@ControllerAdvice)

- Una sola clase global `exception/GlobalExceptionHandler` anotada con `@RestControllerAdvice`.
- Formato de error **estándar** (misma estructura para todos los errores):

```json
{
  "timestamp": "2026-10-11T10:30:00",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Ticket no encontrado con id 42",
  "path": "/api/tickets/42",
  "detalles": ["campo 'asunto': no debe estar vacío"]
}
```

- Excepciones de negocio recomendadas:
  - `ResourceNotFoundException` → HTTP 404
  - `BusinessException` → HTTP 400/409
  - `MethodArgumentNotValidException` (validaciones) → HTTP 400 con lista de campos
  - `AccessDeniedException` → HTTP 403
- **Prohibido** devolver `Map<String,Object>` armado a mano en cada controller, o
  exponer stacktraces al cliente.

---

## 7. DevOps básico

### 7.1 Dockerfile del backend (multi-stage, Maven)

```dockerfile
# ---- Etapa 1: build con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copiar primero el pom para cachear dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Etapa 2: runtime ligero ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Buenas prácticas:**

- Siempre **multi-stage** (imagen final solo con JRE, nunca con Maven ni el JDK completo).
- Copiar `pom.xml` y resolver dependencias antes de copiar `src/` para aprovechar caché.
- Ejecutar como **usuario no root**.
- Añadir un `.dockerignore` que excluya `target/`, `.git/`, `.idea/`.
- (Recomendado) `HEALTHCHECK` y/o Spring Boot Actuator en `/actuator/health`.

### 7.2 Integración en docker-compose

- El servicio `backend` se activa una vez exista su `Dockerfile`.
- `depends_on: db: condition: service_healthy` (usar el `healthcheck` de PostgreSQL).
- La URL de conexión usa el **nombre del servicio** como host:
  `jdbc:postgresql://db:5432/helpdesk_db`.
- Los secretos llegan por **variables de entorno**, no en el código.
- El frontend React se sirve como build estático con **Nginx** (multi-stage Node → Nginx)
  y consume el API vía `VITE_API_BASE_URL`.

Ver el bloque comentado (backend/frontend) en `docker-compose.yml`.

---

## 8. Convenciones de código

- **Idioma:** nombres de clases y código en **inglés**; mensajes al usuario en **español**.
- **Estilo:** seguir Java standard; formateo con `spotless`/IDE por defecto (4 espacios).
- **Lombok:** permitido (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`).
  Inyección de dependencias **por constructor** (usar `@RequiredArgsConstructor`).
- **Endpoints:** en plural y kebab-case → `/api/tickets`, `/api/usuarios`.
- **Versionado:** commits siguiendo **Conventional Commits**
  (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- **Ramas:** `main` (estable) ← `develop` ← `feature/<nombre>`.
- **Tests:** todo `Service` con lógica debe tener tests. Mínimo: unitarios de servicio
  y de controller (MockMvc).

---

## 9. Definition of Done (checklist del PR)

- [ ] Respeta la arquitectura monolítica y las capas.
- [ ] No expone `Entity` en controllers (usa DTO + Mapper).
- [ ] Endpoints protegidos con Spring Security según la matriz de roles.
- [ ] Errores manejados por `@ControllerAdvice` con formato estándar.
- [ ] Entidades alineadas con `database/schema.sql`.
- [ ] Variables sensibles por entorno (no hardcodeadas).
- [ ] Compila (`mvn clean package`), pasa tests y `docker compose up` funciona.
- [ ] Documentación/README actualizados si aplica.

---

## 10. Anti-patrones prohibidos 🚫

1. Lógica de negocio en Controllers o Repositories.
2. Controllers que devuelven `Entity` o `Map` sin tipar.
3. `@Autowired` por campo (inyección por constructor obligatoria).
4. Credenciales o secretos en el código / repositorio.
5. `FetchType.EAGER` indiscriminado o `LazyInitializationException` silenciada con `@JsonIgnore` sin criterio.
6. Consultas N+1 no revisadas.
7. Crear microservicios o dividir el backend en varios proyectos.
8. Devolver stacktraces o detalles internos al cliente.

---

_Última actualización: 2026-10-11._
