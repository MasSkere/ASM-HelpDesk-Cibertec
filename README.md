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
├── frontend/    # Aplicación React
├── database/    # schema.sql, seed.sql
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

### 3. Levantar la base de datos

```bash
docker-compose up -d
```

Esto levanta un contenedor de **PostgreSQL 16** con volumen persistente y ejecuta
automáticamente `database/schema.sql` la primera vez que se crea el volumen.

### 4. Verificar

```bash
docker-compose ps
docker-compose logs -f db
```

Conexión por defecto:

| Parámetro | Valor            |
| --------- | ---------------- |
| Host      | `localhost`      |
| Puerto    | `5432`           |
| Base datos| `helpdesk_db`    |
| Usuario   | `helpdesk_admin` |
| Password  | `helpdesk_pwd`   |

### Comandos útiles

```bash
docker-compose up -d        # Levantar servicios en segundo plano
docker-compose ps           # Ver estado de los contenedores
docker-compose logs -f      # Ver logs en vivo
docker-compose down         # Detener (conserva los datos)
docker-compose down -v      # Detener y borrar datos (¡irreversible!)
```

> ⚠️ Los scripts de `database/` solo se ejecutan con el volumen **vacío**.
> Si cambias el `schema.sql`, ejecuta `docker-compose down -v` y vuelve a levantar.

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
- [ ] Backend Spring Boot (monolito) con capas + Spring Security (JWT).
- [ ] Frontend React con rutas protegidas por rol.
- [ ] Dockerfiles multi-stage e integración en `docker-compose`.
- [ ] Módulo de IA para clasificación/sugerencia de tickets.

---

## 📄 Licencia

Proyecto académico — Instituto Cibertec. Uso educativo.
