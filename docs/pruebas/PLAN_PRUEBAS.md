# Plan de Pruebas QA — ASM HelpDesk

**Responsable:** Jesús — Tester / QA  
**Proyecto:** ASM-HelpDesk-Cibertec  
**Herramienta principal:** Postman  
**Backend bajo prueba:** Java 17 + Spring Boot + Spring Security/JWT + PostgreSQL

## 1. Objetivo

Validar que las APIs implementadas del sistema ASM HelpDesk respondan correctamente, respeten las reglas de negocio y apliquen autorización por rol. Este plan cubre únicamente funcionalidades que existen actualmente en el backend del repositorio; no se marca como aprobada ninguna prueba que no haya sido ejecutada contra un entorno funcional.

## 2. Alcance

Se validarán:

- disponibilidad del backend (`/actuator/health`);
- autenticación y obtención de JWT;
- lectura de categorías;
- creación, consulta, asignación, historial y cambio de estado de tickets;
- restricciones por roles `ESTUDIANTE`, `DOCENTE`, `TECNICO` y `ADMINISTRADOR`;
- consulta de usuarios para administrador;
- reportes KPI, carga de técnicos y tickets sin asignar;
- validaciones negativas (datos inválidos, recursos inexistentes y accesos sin autorización).

No se incluyen todavía pruebas de frontend React ni IBM Watson Assistant porque esos componentes no están implementados en el ZIP revisado.

## 3. Criterios de entrada

Antes de ejecutar la colección Postman deben cumplirse todos los puntos:

1. PostgreSQL levantado y con esquema compatible con el backend.
2. Backend Spring Boot levantado y `GET /actuator/health` respondiendo HTTP 200 con `status=UP`.
3. Existencia de usuarios activos de prueba con roles ADMINISTRADOR, TECNICO, ESTUDIANTE y DOCENTE.
4. Contraseñas de prueba conocidas y almacenadas como BCrypt en la base de datos.
5. Categoría de prueba válida, por defecto `idCategoria=1`.
6. Las vistas `vw_kpi_general`, `vw_carga_tecnicos` y `vw_tickets_sin_asignar` deben existir para ejecutar reportes.

## 4. Estrategia de ejecución

1. Importar `docs/api/ASM-HelpDesk.postman_collection.json`.
2. Importar `docs/api/ASM-Local.postman_environment.json`.
3. Completar únicamente las variables de contraseña. No guardar credenciales reales de producción.
4. Ejecutar primero la carpeta `00 - Precondiciones`.
5. Ejecutar `01 - Autenticación`; los tokens se guardan automáticamente en el Environment.
6. Ejecutar las carpetas restantes en orden.
7. Registrar cada resultado en `MATRIZ_PRUEBAS.csv` como `PASS`, `FAIL` o `BLOCKED`.
8. Para cada fallo nuevo, agregar una incidencia en `INCIDENCIAS_QA.md` y una evidencia en `evidencias/`.

## 5. Criterio PASS / FAIL

- **PASS:** respuesta y reglas coinciden con el resultado esperado.
- **FAIL:** la API responde pero incumple status HTTP, seguridad, validación o regla de negocio.
- **BLOCKED:** la prueba no puede ejecutarse por una dependencia externa o error previo del entorno.
- **NO EJECUTADO:** todavía no se ha realizado la prueba.

## 6. Flujo principal esperado

1. Login del estudiante.
2. Creación del ticket (`REGISTRADO`).
3. Login del administrador.
4. Asignación de técnico.
5. Login del técnico.
6. Consulta de bandeja asignada.
7. Cambio `REGISTRADO -> EN_PROCESO`.
8. Cambio `EN_PROCESO -> RESUELTO`.
9. Cierre `RESUELTO -> CERRADO` por administrador o técnico asignado, según reglas del backend.
10. Consulta del historial y reportes.

## 7. Evidencias mínimas para la entrega EFSRT

Guardar capturas legibles de:

- health del backend;
- login exitoso por al menos un rol;
- creación de ticket HTTP 201;
- asignación de técnico;
- cambio de estado;
- historial del ticket;
- un acceso HTTP 403 por rol incorrecto;
- una validación HTTP 400;
- los tres endpoints de reportes funcionando.

Nombre recomendado: `QA-012_crear_ticket_PASS.png`, `QA-025_usuarios_estudiante_403_PASS.png`, etc.

## 8. Criterio de salida

La fase QA de APIs se considera cerrada cuando:

- las 40 pruebas de la matriz han sido ejecutadas;
- no quedan incidencias BLOQUEADORAS o CRÍTICAS abiertas;
- todos los casos del flujo principal están en PASS;
- las evidencias están versionadas sin credenciales, tokens ni datos personales reales;
- cualquier FAIL aceptado está documentado y aprobado por el equipo.
