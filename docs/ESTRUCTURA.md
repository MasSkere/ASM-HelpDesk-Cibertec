# Estructura del Repositorio — ASM-HelpDesk-Cibertec

Arquitectura: **Monolito** (un único backend Spring Boot) + SPA React + PostgreSQL.
Todo se orquesta con **Docker / Docker Compose**.

```text
ASM-HelpDesk-Cibertec/
│
├── .gitignore
├── .env.example                  # Variables de entorno de ejemplo (NO subir .env real)
├── docker-compose.yml            # Orquestación de contenedores (BD + backend + frontend)
├── README.md                     # Descripción general y arranque rápido
├── INSTRUCCIONES_AGENTE.md       # Reglas de desarrollo / System Prompt para agentes IA
│
├── database/                     # Scripts SQL de la base de datos
│   ├── schema.sql                # DDL: creación de tablas, constraints e índices
│   └── seed.sql                  # (opcional) Datos de prueba / catálogos iniciales
│
├── docs/                         # Documentación técnica del proyecto
│   ├── ESTRUCTURA.md             # Este archivo
│   ├── arquitectura.md           # Decisiones de arquitectura (ADR)
│   └── api/                      # Colecciones Postman / contratos de API
│
├── backend/                      # Monolito Java Spring Boot
│   ├── Dockerfile                # Build multi-stage (Maven -> JRE)
│   ├── .dockerignore
│   ├── pom.xml                   # (o build.gradle si se usa Gradle)
│   └── src/
│       ├── main/
│       │   ├── java/com/cibertec/helpdesk/
│       │   │   ├── AsmHelpDeskApplication.java   # @SpringBootApplication (main)
│       │   │   │
│       │   │   ├── controller/       # Capa de entrada: @RestController (Reciben/Devuelven DTOs)
│       │   │   │   ├── AuthController.java
│       │   │   │   ├── UsuarioController.java
│       │   │   │   ├── CategoriaController.java
│       │   │   │   └── TicketController.java
│       │   │   │
│       │   │   ├── service/          # Capa de negocio: interfaces de servicio
│       │   │   │   ├── UsuarioService.java
│       │   │   │   ├── CategoriaService.java
│       │   │   │   ├── TicketService.java
│       │   │   │   └── impl/         # Implementaciones (@Service)
│       │   │   │       ├── UsuarioServiceImpl.java
│       │   │   │       ├── CategoriaServiceImpl.java
│       │   │   │       └── TicketServiceImpl.java
│       │   │   │
│       │   │   ├── repository/       # Capa de datos: interfaces JpaRepository
│       │   │   │   ├── UsuarioRepository.java
│       │   │   │   ├── CategoriaRepository.java
│       │   │   │   └── TicketRepository.java
│       │   │   │
│       │   │   ├── entity/           # Entidades JPA (Mapeo 1:1 con las tablas)
│       │   │   │   ├── Usuario.java
│       │   │   │   ├── Categoria.java
│       │   │   │   ├── Ticket.java
│       │   │   │   ├── Rol.java             # Enums de dominio (alineados a los CHECK del schema)
│       │   │   │   ├── TipoSolicitud.java
│       │   │   │   ├── Prioridad.java
│       │   │   │   └── EstadoTicket.java
│       │   │   │
│       │   │   ├── dto/              # Objetos de transferencia (NUNCA exponer Entities)
│       │   │   │   ├── request/      # Lo que entra al API
│       │   │   │   │   ├── UsuarioRequest.java
│       │   │   │   │   ├── CategoriaRequest.java
│       │   │   │   │   ├── TicketRequest.java
│       │   │   │   │   └── LoginRequest.java
│       │   │   │   └── response/     # Lo que sale del API
│       │   │   │       ├── UsuarioResponse.java
│       │   │   │       ├── CategoriaResponse.java
│       │   │   │       ├── TicketResponse.java
│       │   │   │       └── ApiErrorResponse.java
│       │   │   │
│       │   │   ├── mapper/           # Conversión Entity <-> DTO (MapStruct/manual)
│       │   │   │   ├── UsuarioMapper.java
│       │   │   │   ├── CategoriaMapper.java
│       │   │   │   └── TicketMapper.java
│       │   │   │
│       │   │   ├── security/         # Spring Security + JWT
│       │   │   │   ├── SecurityConfig.java       # SecurityFilterChain, roles por ruta CORS/BCrypt
│       │   │   │   ├── UserDetailsServiceImpl.java
│       │   │   │   ├── jwt/
│       │   │   │   │   ├── JwtService.java        # Generación/validación de tokens
│       │   │   │   │   └── JwtAuthenticationFilter.java
│       │   │   │   └── model/                     # Roles / enums de seguridad (opcional)
│       │   │   │
│       │   │   ├── exception/        # Manejo global de errores
│       │   │   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│       │   │   │   ├── ResourceNotFoundException.java
│       │   │   │   └── BusinessException.java
│       │   │   │
│       │   │   ├── config/           # Beans de configuración (OpenAPI, CORS, etc.)
│       │   │   │   ├── OpenApiConfig.java
│       │   │   │   └── CorsConfig.java
│       │   │   │
│       │   │   └── util/             # Utilidades transversales
│       │   │       └── GeneradorCodigoTicket.java   # ej. "TCK-2026-0001"
│       │   │
│       │   └── resources/
│       │       ├── application.yml         # Config base
│       │       ├── application-dev.yml     # Perfil local
│       │       ├── application-prod.yml    # Perfil prod (Docker)
│       │       └── db/migration/           # Migraciones Flyway (opcional)
│       │
│       └── test/
│           └── java/com/cibertec/helpdesk/
│               ├── service/
│               └── controller/
│
└── frontend/                      # Aplicación React (SPA)
    ├── Dockerfile                # Build multi-stage (Node -> Nginx)
    ├── .dockerignore
    ├── package.json
    ├── vite.config.js            # (o CRA si aplica)
    ├── public/
    └── src/
        ├── assets/               # Imágenes, estilos, iconos
        ├── components/           # Componentes reutilizables (UI)
        ├── pages/                # Vistas por ruta (Login, Dashboard, Tickets...)
        ├── services/             # Consumo de API (Axios + manejo de JWT)
        ├── hooks/                # Custom hooks (useAuth, useFetch...)
        ├── context/              # React Context (AuthContext, ThemeContext...)
        ├── routes/               # Enrutado y rutas protegidas por rol
        └── utils/                # Helpers (formateo de fechas, validaciones)
```

## Notas de la estructura

- **Un solo backend (monolito):** todos los módulos (`controller`, `service`, `repository`,
  `entity`, `dto`, `mapper`, `security`) viven dentro del mismo proyecto y del mismo
  artefacto desplegable (`backend`).
- **Capas unidireccionales:** `Controller -> Service -> Repository -> Entity`.
  El `mapper` traduce entre `entity` y `dto`; los DTOs son la única frontera pública del API.
- **`database/`** es independiente del backend para poder versionar los scripts SQL y
  montarlos en el `docker-entrypoint-initdb.d` de PostgreSQL.
- Los directorios vacíos se mantienen con un `.gitkeep` hasta que contengan código real.
