# QA / Tester — Jesús

Esta carpeta contiene los entregables del rol **Tester** definidos en el README del proyecto.

## Archivos

- `../api/ASM-HelpDesk.postman_collection.json`: colección Postman con 40 casos de prueba.
- `../api/ASM-Local.postman_environment.json`: variables del entorno local; contraseñas y tokens quedan vacíos.
- `PLAN_PRUEBAS.md`: alcance, estrategia, criterios y flujo principal.
- `MATRIZ_PRUEBAS.csv`: trazabilidad de todos los casos y espacio para resultado real.
- `INCIDENCIAS_QA.md`: hallazgos QA detectados durante la revisión del repositorio.
- `evidencias/`: capturas y evidencia de las ejecuciones.

## Antes de ejecutar

El entorno actual del repositorio presenta bloqueos documentados en `INCIDENCIAS_QA.md`. En especial, el Compose no crea todavía todas las estructuras requeridas por el backend y no existe un juego de usuarios con contraseñas QA utilizables. Por eso los casos quedan inicialmente en `NO EJECUTADO`; no se han inventado resultados.

## Cómo ejecutar en Postman

1. Importar la colección y el Environment de `docs/api/`.
2. Pedir al equipo credenciales **de prueba** para los cuatro roles.
3. Completar `admin_password`, `tecnico_password`, `estudiante_password` y `docente_password` solo en el Environment local de Postman.
4. Ejecutar carpetas de `00` a `05` en orden.
5. Copiar el resultado a `MATRIZ_PRUEBAS.csv`.
6. Si falla una prueba, documentarla en `INCIDENCIAS_QA.md` y guardar evidencia.

## Convención de evidencias

`QA-<ID>_<descripcion>_<PASS|FAIL>.png`

Ejemplos:

- `QA-012_crear_ticket_PASS.png`
- `QA-025_usuarios_estudiante_403_PASS.png`
- `QA-030_reporte_kpi_FAIL.png`

## Seguridad

No versionar:

- tokens JWT activos;
- `.env` real;
- contraseñas reales;
- secretos de IBM/Cloud;
- datos personales reales.
