# Incidencias QA — ASM HelpDesk

**Responsable QA:** Jesús  
**Estado del documento:** revisión estática inicial del repositorio.  
**Importante:** estas incidencias se detectaron comparando configuración, SQL y código fuente; todavía deben verificarse en ejecución cuando exista un entorno levantable.

## QA-BLK-001 — El esquema que monta Docker no contiene `historial_ticket`

- **Severidad:** BLOQUEADOR
- **Estado:** ABIERTO
- **Componente:** Base de datos / arranque backend
- **Evidencia estática:** `docker-compose.yml` monta `database/schema.sql`; `HistorialTicket.java` usa `@Table(name = "historial_ticket")`; esa tabla no existe en `schema.sql`.
- **Impacto esperado:** `spring.jpa.hibernate.ddl-auto=validate` puede impedir el arranque del backend al detectar que falta la tabla.
- **Resultado esperado:** el esquema inicializado por Docker debe contener todas las tablas mapeadas por JPA.
- **Sugerencia al responsable BD/Backend:** unificar el script usado por Docker con el esquema avanzado o migraciones versionadas.

## QA-BLK-002 — `codigo` del ticket no tiene generación en `schema.sql`

- **Severidad:** BLOQUEADOR
- **Estado:** ABIERTO
- **Componente:** Base de datos / creación de tickets
- **Evidencia estática:** `Ticket.codigo` está configurado `insertable=false` y el servicio no asigna el código; `schema.sql` define `codigo VARCHAR(20) NOT NULL` sin valor por defecto. El script `asm_helpdesk_postgres16.sql` sí incorpora generación automática.
- **Impacto esperado:** al crear tickets contra el `schema.sql` actual, PostgreSQL puede rechazar el INSERT por `codigo` nulo.
- **Resultado esperado:** todo ticket obtiene automáticamente un código único.

## QA-CRI-003 — Los reportes dependen de vistas que Docker no crea

- **Severidad:** CRÍTICA
- **Estado:** ABIERTO
- **Componente:** Reportes
- **Evidencia estática:** `ReporteService` consulta `vw_kpi_general`, `vw_carga_tecnicos` y `vw_tickets_sin_asignar`; se crean en `database/procesos.sql`, pero ese archivo no está montado en `docker-compose.yml`.
- **Impacto esperado:** `/api/reportes/kpi`, `/carga-tecnicos` y `/sin-asignar` fallarán en una base recién creada mediante el Compose actual.
- **Resultado esperado:** el entorno inicial debe crear las vistas antes de usar reportes.

## QA-CRI-004 — No hay usuarios de prueba autenticables listos para Postman

- **Severidad:** CRÍTICA
- **Estado:** ABIERTO
- **Componente:** Datos de prueba / autenticación
- **Evidencia estática:** `database/datos_prueba.sql` contiene `$2a$10$REEMPLAZAR_HASH_BCRYPT` y no se ejecuta desde `docker-compose.yml`.
- **Impacto:** no se pueden ejecutar las pruebas autenticadas con un entorno nuevo sin preparación manual.
- **Resultado esperado:** ambiente QA con usuarios activos y hashes BCrypt válidos para los cuatro roles.
- **Nota:** no se deben subir contraseñas de producción ni secretos al repositorio.

## QA-MED-005 — `TicketController` accede directamente a Repository y expone Entity

- **Severidad:** MEDIA
- **Estado:** ABIERTO
- **Componente:** Arquitectura backend
- **Evidencia estática:** `TicketController` inyecta `UsuarioRepository` y varios endpoints devuelven `Ticket`; `INSTRUCCIONES_AGENTE.md` exige `Controller -> Service -> Repository -> Entity` y prohíbe exponer entidades.
- **Impacto:** incumplimiento de las reglas internas del repositorio y mayor acoplamiento de la API.
- **Resultado esperado:** Controller usa Service y DTOs de respuesta.

## QA-MED-006 — `CategoriaController` expone `Categoria` directamente

- **Severidad:** MEDIA
- **Estado:** ABIERTO
- **Componente:** Arquitectura backend
- **Evidencia estática:** endpoints de categorías devuelven `Categoria`, contrario a la regla obligatoria de DTOs del repositorio.
- **Resultado esperado:** respuestas mediante `CategoriaResponse`/mapper.

## QA-MED-007 — Inconsistencia de flujo al asignar técnico

- **Severidad:** MEDIA
- **Estado:** POR CONFIRMAR
- **Componente:** Tickets
- **Evidencia estática:** `database/procesos.sql` indica que al asignar un técnico a un ticket `REGISTRADO` se cambia a `EN_PROCESO`; `TicketService.asignarTecnico()` solo modifica `idTecnico`.
- **Impacto posible:** el mismo proceso puede producir estados diferentes según se ejecute por función SQL o API Java.
- **Resultado esperado:** una única regla de negocio acordada y aplicada en todos los caminos.

## Registro de nuevas incidencias

Para agregar una nueva incidencia usar:

```text
ID: QA-XXX-000
Severidad: BLOQUEADOR / CRÍTICA / ALTA / MEDIA / BAJA
Estado: ABIERTO / EN CORRECCIÓN / RESUELTO / RECHAZADO
Endpoint:
Precondiciones:
Pasos:
Resultado actual:
Resultado esperado:
Evidencia:
Responsable:
```
