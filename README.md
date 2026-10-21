# ASM-HelpDesk-Cibertec

Sistema de Mesa de Ayuda y Soporte Técnico con IA para el Instituto.

---

## 📌 Descripción

**ASM-HelpDesk-Cibertec** es una plataforma web para la gestión de incidencias y
solicitudes de soporte técnico (mesa de ayuda). Permite a **estudiantes** y **docentes**
registrar tickets, a los **técnicos** atenderlos y asignarles prioridad/estado, y a los
**administradores** gestionar usuarios, categorías y supervisar la operación completa.

El proyecto se construye como un **monolito** (un único backend desplegable) con
**frontend React** y **base de datos PostgreSQL**, todo orquestado con **Docker Compose**.

---

## 🧱 Arquitectura

```text
Monolito (Spring Boot)  ──REST──▶  Frontend React
        │
        └──JPA/JDBC──▶  PostgreSQL 16
```

| Capa          | Tecnología                          |
| ------------- | ----------------------------------- |
| Frontend      | React (Vite) + Axios + React Router |
| Backend       | Java 17+ · Spring Boot 3.x (Monolito)|
| Persistencia  | Spring Data JPA (Hibernate)         |
| Base de Datos | PostgreSQL 16 (15+)                 |
| Seguridad     | Spring Security + JWT               |
| Build Backend | Maven (multi-stage Docker)          |
| Infraestructura | Docker + Docker Compose           |

> Ver [`docs/ESTRUCTURA.md`](docs/ESTRUCTURA.md) para el árbol completo de carpetas y
> [`INSTRUCCIONES_AGENTE.md`](INSTRUCCIONES_AGENTE.md) para las reglas de desarrollo.

---

## 👥 Roles del sistema

| Rol           | Descripción                                                        |
| ------------- | ------------------------------------------------------------------ |
| `ESTUDIANTE`  | Registra tickets y consulta el estado de los suyos.                |
| `DOCENTE`     | Igual que estudiante; puede priorizar sus solicitudes.             |
| `TECNICO`     | Atiende y actualiza tickets asignados.                             |
| `ADMINISTRADOR` | Gestión total: usuarios, categorías, asignaciones y reportes.    |

---

## 🗂️ Estructura de carpetas (resumen)

```text
ASM-HelpDesk-Cibertec/
├── backend/     # Monolito Spring Boot (controller, service, repository, entity, dto, mapper, security)
├── frontend/    # Aplicación React (Vite + React Router + Axios + Nginx)
├── database/    # schema.sql (esquema completo) y datos_prueba.sql (seed)
├── docs/        # Documentación técnica
├── docker-compose.yml
└── INSTRUCCIONES_AGENTE.md
```

---

## 🚀 Puesta en marcha

### Requisitos previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (incluye Docker Compose)
- Git

### 1. Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd ASM-HelpDesk-Cibertec
```

### 2. (Opcional) Configurar variables de entorno

Copia el archivo de ejemplo y ajusta los valores:

```bash
cp .env.example .env
```

### 3. Levantar el sistema completo

```bash
docker compose up -d --build
```

Esto construye y levanta los **tres servicios**:

| Servicio   | Contenedor               | URL                     |
| ---------- | ------------------------ | ----------------------- |
| `db`       | `asm-helpdesk-db`        | `localhost:5432`        |
| `backend`  | `asm-helpdesk-backend`   | `http://localhost:8080` |
| `frontend` | `asm-helpdesk-frontend`  | `http://localhost:3000` |

La primera vez, PostgreSQL ejecuta automáticamente `database/schema.sql` y
`database/datos_prueba.sql` (volumen vacío).

### 4. Verificar

```bash
docker compose ps
docker compose logs -f backend
```

Abre **http://localhost:3000** e inicia sesión con una cuenta de prueba:

| Rol           | Correo                      | Contraseña    |
| ------------- | --------------------------- | ------------- |
| Administrador | `admin@cibertec.edu.pe`     | `Admin123!`   |
| Técnico       | `tecnico1@cibertec.edu.pe`  | `Tecnico123!` |
| Técnico       | `tecnico2@cibertec.edu.pe`  | `Tecnico123!` |
| Docente       | `docente@cibertec.edu.pe`   | `Docente123!` |
| Estudiante    | `alumno@cibertec.edu.pe`    | `Alumno123!`  |

> Las contraseñas se almacenan como hashes **BCrypt** (`$2a$`, coste 10).

Conexión directa a la base de datos:

| Parámetro | Valor            |
| --------- | ---------------- |
| Host      | `localhost`      |
| Puerto    | `5432`           |
| Base datos| `helpdesk_db`    |
| Usuario   | `helpdesk_admin` |
| Password  | `helpdesk_pwd`   |

### 5. Desarrollo del frontend (sin Docker)

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173 (proxy /api -> localhost:8080)
```


### Comandos útiles

```bash
docker compose up -d --build  # Construir y levantar todo
docker compose ps             # Ver estado de los contenedores
docker compose logs -f        # Ver logs en vivo
docker compose down           # Detener (conserva los datos)
docker compose down -v        # Detener y borrar datos (¡irreversible!)
```

> ⚠️ Los scripts de `database/` solo se ejecutan con el volumen **vacío**.
> Si cambias el `schema.sql`, ejecuta `docker-compose down -v` y vuelve a levantar.

---

## 🔌 API REST (resumen)

Base URL: `http://localhost:8080/api` (o `http://localhost:3000/api` vía Nginx).

| Método | Ruta | Roles | Descripción |
| ------ | ---- | ----- | ----------- |
| POST | `/auth/login` | público | Inicia sesión y devuelve JWT |
| POST | `/auth/register` | público | Registro (rol `ESTUDIANTE`) |
| GET | `/tickets` | autenticado | Bandeja según rol |
| GET | `/tickets/mios` | Estudiante, Docente | Mis tickets |
| GET | `/tickets/asignados` | Técnico | Tickets asignados |
| GET | `/tickets/{id}` | autenticado (con visibilidad) | Detalle |
| GET | `/tickets/{id}/historial` | autenticado (con visibilidad) | Bitácora |
| POST | `/tickets` | Estudiante, Docente | Crear ticket |
| PATCH | `/tickets/{id}/estado` | Técnico, Admin | Cambiar estado |
| PATCH | `/tickets/{id}/asignar` | Admin | Asignar técnico |
| GET | `/categorias` | autenticado | Listar categorías |
| POST/PUT/DELETE | `/categorias[/{id}]` | Admin | CRUD de categorías |
| GET | `/usuarios` | Admin | Listar usuarios |
| GET | `/reportes/kpi` | Admin | KPIs generales |
| GET | `/reportes/carga-tecnicos` | Admin | Carga por técnico |
| GET | `/reportes/sin-asignar` | Admin | Tickets pendientes |
| POST | `/chatbot/chat` | autenticado | Consulta al asistente IA |
| POST | `/chatbot/sugerir-ticket` | autenticado | Sugiere tipo, prioridad y categoría |

Los errores usan el formato estándar:
`{ timestamp, status, error, message, path, detalles[] }`.

---

## 🤖 Asistente IA (IBM watsonx.ai)

El sistema incluye un asistente IA con dos funciones:

- **Chat flotante** (botón 🤟 en la esquina inferior derecha): consulta general
  en lenguaje natural sobre la mesa de ayuda.
- **"✨ Sugerir con IA"** (pantalla *Nuevo ticket*): clasifica el ticket
  (tipo de solicitud, prioridad y categoría) a partir del asunto y la
  descripción escritos por el usuario.

Ambos responden con un campo `origen`: `WATSONX` (IBM) o `LOCAL`
(asistente heurístico de respaldo). **Si no hay credenciales configuradas o
IBM no responde, el sistema usa automáticamente el asistente local**, por lo
que la funcionalidad nunca se cae.

### Configuración de credenciales (importante 🔒)

Las credenciales de IBM **nunca se escriben en el código ni en el repositorio**: se
guardan solo en el archivo local `.env` (ignorado por Git, verificado con
`git check-ignore`).

1. Copia la plantilla (si aún no lo hiciste):

   ```bash
   cp .env.example .env
   ```

2. Abre `.env` y pega tus valores en la sección **Asistente IA — IBM watsonx.ai**:

   | Variable | Descripción |
   | -------- | ----------- |
   | `WATSONX_APIKEY` | Clave larga que generaste en IBM Cloud. |
   | `WATSONX_PROJECT_ID` | Código de la pestaña *General* de tu proyecto watsonx.ai. |
   | `WATSONX_URL` | URL de tu región (por defecto `us-south`). |
   | `WATSONX_MODEL` | Modelo elegido (granite o llama). |

3. Reinicia el backend para que tome los valores:

   ```bash
   docker compose up -d --build backend
   ```

4. Verifica en los logs (`docker compose logs -f backend`) y en la respuesta
   del chatbot: si dice `origen: WATSONX`, la integración funciona; si dice
   `origen: LOCAL`, revisa las credenciales.

---

## 👥 Equipo y distribución de tareas

Proyecto colaborativo con roles bien definidos:

| Integrante | Rol | Responsabilidad |
| ---------- | --- | --------------- |
| **Edson** | BD | Crea las tablas en PostgreSQL. |
| **Frank** | Backend | Conecta Java a esas tablas y crea los "enchufes" (APIs). |
| **Jesús** | Tester | Prueba esos enchufes con Postman para confirmar que pasan corriente (datos). |
| **Ronal** | Frontend + IA | Diseña la "lámpara" (la web), conecta los cables a los enchufes de Frank y añade el Chatbot inteligente de IBM. |

> 💡 **Regla de oro del equipo:** cada uno trabaja en su frente, respetando la
> estructura del repositorio y las reglas de [`INSTRUCCIONES_AGENTE.md`](INSTRUCCIONES_AGENTE.md).

---

## 🛣️ Roadmap

- [x] Definición de arquitectura y estructura de carpetas.
- [x] Orquestación de PostgreSQL con Docker Compose.
- [x] Esquema completo (`schema.sql`) con triggers, funciones y vistas de reportes.
- [x] Backend Spring Boot (monolito) con capas, DTOs y Spring Security (JWT).
- [x] Endpoints de tickets, categorías, usuarios y reportes.
- [x] Frontend React con rutas protegidas por rol y CRUD administrativo.
- [x] Dockerfiles multi-stage e integración de los 3 servicios en `docker-compose`.
- [x] Módulo de IA (chatbot IBM watsonx) para clasificación/sugerencia de tickets.

---

## 📄 Licencia

Proyecto académico — Instituto Cibertec. Uso educativo.
