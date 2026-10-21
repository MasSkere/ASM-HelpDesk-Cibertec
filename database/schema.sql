-- =============================================================================
--  ASM-HelpDesk-Cibertec — Esquema de Base de Datos (PostgreSQL 15+)
--  ---------------------------------------------------------------------------
--  ESTE ARCHIVO ES LA FUENTE DE VERDAD ÚNICA del modelo de datos.
--  docker-compose.yml lo monta en /docker-entrypoint-initdb.d/, por lo que se
--  ejecuta automáticamente la PRIMERA vez que se crea el volumen de PostgreSQL.
--
--  Incluye TODO lo que el backend Spring Boot necesita arrancar:
--    1. usuarios          (sin dependencias)
--    2. categorias        (sin dependencias)
--    3. tickets           (depende de usuarios y categorias)
--    4. historial_ticket  (depende de tickets; se llena sola por trigger)
--    5. Triggers          (validación de flujo + bitácora de historial)
--    6. Funciones         (asignación, cambio de estado, reportes)
--    7. Vistas            (KPIs y carga de técnicos → usadas por ReporteService)
--
--  Historial: la tabla se ESCRIBE desde PostgreSQL mediante el trigger
--  `trg_tickets_historial`. El backend solo la LEE
--  (HistorialTicketService.listarHistorial).
--
--  Ejecución manual:
--    psql -h localhost -U helpdesk_admin -d helpdesk_db -f database/schema.sql
--
--  IMPORTANTE: los scripts SOLO se ejecutan con el volumen VACÍO.
--  Si cambias este script, ejecuta `docker compose down -v` y vuelve a levantar.
--
--  Archivos hermanos (ya no son necesarios para arrancar, quedan como
--  referencia histórica del trabajo de la fase de BD):
--    database/asm_helpdesk_postgres16.sql  -> versión previa de las tablas
--    database/procesos.sql                 -> versión previa de funciones/vistas
-- =============================================================================

BEGIN;

-- Re-ejecutable en desarrollo
DROP VIEW  IF EXISTS vw_kpi_general;
DROP VIEW  IF EXISTS vw_carga_tecnicos;
DROP VIEW  IF EXISTS vw_tickets_sin_asignar;
DROP VIEW  IF EXISTS vw_tickets_detalle;
DROP VIEW  IF EXISTS vw_resumen_estado_prioridad;

DROP TABLE IF EXISTS historial_ticket CASCADE;
DROP TABLE IF EXISTS tickets         CASCADE;
DROP TABLE IF EXISTS categorias      CASCADE;
DROP TABLE IF EXISTS usuarios        CASCADE;

DROP FUNCTION IF EXISTS fn_tickets_validar()        CASCADE;
DROP FUNCTION IF EXISTS fn_tickets_historial()      CASCADE;
DROP FUNCTION IF EXISTS fn_asignar_tecnico(INTEGER, INTEGER) CASCADE;
DROP FUNCTION IF EXISTS fn_cambiar_estado(INTEGER, VARCHAR)  CASCADE;
DROP FUNCTION IF EXISTS fn_asignar_tecnico_auto(INTEGER)     CASCADE;
DROP FUNCTION IF EXISTS fn_reporte_tickets(DATE, DATE)       CASCADE;

DROP SEQUENCE IF EXISTS seq_tickets_codigo CASCADE;

-- =============================================================================
-- 1. USUARIOS
-- =============================================================================
CREATE TABLE usuarios (
    id_usuario  INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombres     VARCHAR(100) NOT NULL,
    apellidos   VARCHAR(100) NOT NULL,
    correo      VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,                 -- Hash BCrypt (NUNCA texto plano)
    rol         VARCHAR(20)  NOT NULL,
    estado      BOOLEAN      NOT NULL DEFAULT TRUE,    -- TRUE = activo, FALSE = inactivo

    CONSTRAINT chk_usuarios_rol
        CHECK (rol IN ('ESTUDIANTE', 'DOCENTE', 'TECNICO', 'ADMINISTRADOR')),
    CONSTRAINT chk_usuarios_correo
        CHECK (correo = btrim(correo) AND position('@' IN correo) > 1)
);

-- Unicidad del correo INSENSIBLE a mayúsculas (un UNIQUE simple distinguiría
-- "Ana@x.com" de "ana@x.com"). Además sirve para el login por correo.
CREATE UNIQUE INDEX uq_usuarios_correo ON usuarios (lower(correo));

-- Útil para listar técnicos activos al asignar tickets
CREATE INDEX idx_usuarios_rol_activo ON usuarios (rol) WHERE estado;

COMMENT ON TABLE  usuarios          IS 'Usuarios del sistema (solicitantes, técnicos y administradores)';
COMMENT ON COLUMN usuarios.password IS 'Contraseña cifrada con BCrypt';
COMMENT ON COLUMN usuarios.rol      IS 'ESTUDIANTE | DOCENTE | TECNICO | ADMINISTRADOR';
COMMENT ON COLUMN usuarios.estado   IS 'TRUE = activo, FALSE = inactivo';

-- =============================================================================
-- 2. CATEGORIAS
-- =============================================================================
CREATE TABLE categorias (
    id_categoria INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  TEXT,

    CONSTRAINT chk_categorias_nombre CHECK (btrim(nombre) <> '')
);

CREATE UNIQUE INDEX uq_categorias_nombre ON categorias (lower(nombre));

COMMENT ON TABLE categorias IS 'Clasificación de los tickets (Hardware, Software, Redes, Cuentas, etc.)';

-- =============================================================================
-- 3. TICKETS
-- =============================================================================
-- Secuencia para el código legible: TCK-<año>-<correlativo de 4 dígitos>
CREATE SEQUENCE seq_tickets_codigo START 1;

CREATE TABLE tickets (
    id_ticket           INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo              VARCHAR(20)  NOT NULL
                        DEFAULT ('TCK-' || to_char(CURRENT_DATE, 'YYYY') || '-'
                                 || lpad(nextval('seq_tickets_codigo')::text, 4, '0')),
    asunto              VARCHAR(150) NOT NULL,
    descripcion         TEXT,
    tipo_solicitud      VARCHAR(30)  NOT NULL,
    prioridad           VARCHAR(20)  NOT NULL DEFAULT 'MEDIA',
    estado              VARCHAR(20)  NOT NULL DEFAULT 'REGISTRADO',
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion    TIMESTAMP,                      -- se llena al pasar a RESUELTO
    id_solicitante      INTEGER      NOT NULL,          -- Usuario que reporta
    id_tecnico          INTEGER,                        -- Técnico asignado (NULL si aún no)
    id_categoria        INTEGER      NOT NULL,

    CONSTRAINT uq_tickets_codigo UNIQUE (codigo),

    CONSTRAINT chk_tickets_asunto CHECK (btrim(asunto) <> ''),
    CONSTRAINT chk_tickets_tipo
        CHECK (tipo_solicitud IN ('INCIDENTE', 'REQUERIMIENTO', 'CONSULTA')),
    CONSTRAINT chk_tickets_prioridad
        CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT chk_tickets_estado
        CHECK (estado IN ('REGISTRADO', 'EN_PROCESO', 'RESUELTO', 'CERRADO', 'CANCELADO')),
    CONSTRAINT chk_tickets_tecnico_distinto
        CHECK (id_tecnico IS NULL OR id_tecnico <> id_solicitante),
    CONSTRAINT chk_tickets_fechas
        CHECK (fecha_resolucion IS NULL OR fecha_resolucion >= fecha_creacion),

    CONSTRAINT fk_tickets_solicitante
        FOREIGN KEY (id_solicitante) REFERENCES usuarios (id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_tecnico
        FOREIGN KEY (id_tecnico) REFERENCES usuarios (id_usuario)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_tickets_categoria
        FOREIGN KEY (id_categoria) REFERENCES categorias (id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

ALTER SEQUENCE seq_tickets_codigo OWNED BY tickets.codigo;

COMMENT ON TABLE  tickets                  IS 'Tickets de soporte técnico (mesa de ayuda)';
COMMENT ON COLUMN tickets.codigo           IS 'Código legible único, ej. TCK-2026-0001 (lo genera la BD)';
COMMENT ON COLUMN tickets.tipo_solicitud   IS 'INCIDENTE | REQUERIMIENTO | CONSULTA';
COMMENT ON COLUMN tickets.prioridad        IS 'BAJA | MEDIA | ALTA | CRITICA';
COMMENT ON COLUMN tickets.estado           IS 'REGISTRADO | EN_PROCESO | RESUELTO | CERRADO | CANCELADO';
COMMENT ON COLUMN tickets.id_tecnico       IS 'Técnico asignado; NULL mientras no se asigne';
COMMENT ON COLUMN tickets.fecha_resolucion IS 'Momento en que pasó a RESUELTO; base del tiempo de resolución';

-- Índices (el de codigo ya existe por el UNIQUE).
-- PostgreSQL NO indexa FKs automáticamente, por eso se crean explícitamente.
CREATE INDEX idx_tickets_solicitante    ON tickets (id_solicitante);
CREATE INDEX idx_tickets_tecnico        ON tickets (id_tecnico) WHERE id_tecnico IS NOT NULL;
CREATE INDEX idx_tickets_categoria      ON tickets (id_categoria);
CREATE INDEX idx_tickets_estado_prior   ON tickets (estado, prioridad);   -- bandejas / filtros
CREATE INDEX idx_tickets_fecha_creacion ON tickets (fecha_creacion DESC);

-- -----------------------------------------------------------------------------
-- Reglas que un CHECK no puede expresar (referencian otras tablas / el cambio)
-- -----------------------------------------------------------------------------
CREATE FUNCTION fn_tickets_validar() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    -- El técnico asignado debe tener rol TECNICO y estar activo
    IF NEW.id_tecnico IS NOT NULL AND
       NOT EXISTS (SELECT 1 FROM usuarios
                   WHERE id_usuario = NEW.id_tecnico AND rol = 'TECNICO' AND estado) THEN
        RAISE EXCEPTION 'El usuario % no es un técnico activo', NEW.id_tecnico
            USING ERRCODE = 'check_violation';
    END IF;

    -- Flujo de estados permitido (CERRADO y CANCELADO son finales)
    IF TG_OP = 'UPDATE' AND NEW.estado <> OLD.estado AND NOT (
           (OLD.estado = 'REGISTRADO' AND NEW.estado IN ('EN_PROCESO', 'CANCELADO'))
        OR (OLD.estado = 'EN_PROCESO' AND NEW.estado IN ('RESUELTO', 'CANCELADO'))
        OR (OLD.estado = 'RESUELTO'   AND NEW.estado IN ('CERRADO', 'EN_PROCESO'))) THEN
        RAISE EXCEPTION 'Transición de estado no permitida: % -> %', OLD.estado, NEW.estado
            USING ERRCODE = 'check_violation';
    END IF;

    -- Reabrir un ticket resuelto: limpia la fecha de resolución
    IF TG_OP = 'UPDATE' AND OLD.estado = 'RESUELTO' AND NEW.estado = 'EN_PROCESO' THEN
        NEW.fecha_resolucion := NULL;
    END IF;

    -- Auditoría de tiempos
    NEW.fecha_actualizacion := CURRENT_TIMESTAMP;
    IF NEW.estado = 'RESUELTO' AND NEW.fecha_resolucion IS NULL THEN
        NEW.fecha_resolucion := CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_tickets_validar
    BEFORE INSERT OR UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION fn_tickets_validar();

-- =============================================================================
-- 4. HISTORIAL_TICKET  (RN-07: todo cambio importante queda registrado)
-- =============================================================================
CREATE TABLE historial_ticket (
    id_historial    INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_ticket       INTEGER      NOT NULL,
    accion          VARCHAR(20)  NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20),
    id_tecnico      INTEGER,                       -- técnico vigente tras el cambio
    fecha           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_historial_accion
        CHECK (accion IN ('CREADO', 'CAMBIO_ESTADO', 'ASIGNACION')),
    CONSTRAINT fk_historial_ticket
        FOREIGN KEY (id_ticket) REFERENCES tickets (id_ticket)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_historial_ticket_fecha ON historial_ticket (id_ticket, fecha DESC);

COMMENT ON TABLE historial_ticket IS 'Bitácora automática de creación, cambios de estado y asignaciones de tickets';

-- La bitácora la escribe PostgreSQL; el backend solo la lee.
CREATE FUNCTION fn_tickets_historial() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO historial_ticket (id_ticket, accion, estado_nuevo, id_tecnico)
        VALUES (NEW.id_ticket, 'CREADO', NEW.estado, NEW.id_tecnico);
    ELSE
        IF NEW.estado <> OLD.estado THEN
            INSERT INTO historial_ticket (id_ticket, accion, estado_anterior, estado_nuevo, id_tecnico)
            VALUES (NEW.id_ticket, 'CAMBIO_ESTADO', OLD.estado, NEW.estado, NEW.id_tecnico);
        END IF;
        IF NEW.id_tecnico IS DISTINCT FROM OLD.id_tecnico THEN
            INSERT INTO historial_ticket (id_ticket, accion, estado_anterior, estado_nuevo, id_tecnico)
            VALUES (NEW.id_ticket, 'ASIGNACION', OLD.estado, NEW.estado, NEW.id_tecnico);
        END IF;
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER trg_tickets_historial
    AFTER INSERT OR UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION fn_tickets_historial();

-- =============================================================================
-- 5. FUNCIONES (procesos de negocio en BD, útiles para reportes y Postman)
-- =============================================================================

-- CU-15: asignar o reasignar técnico. Si el ticket está REGISTRADO pasa a EN_PROCESO.
CREATE OR REPLACE FUNCTION fn_asignar_tecnico(p_id_ticket INTEGER, p_id_tecnico INTEGER)
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE v_estado VARCHAR(20);
BEGIN
    SELECT estado INTO v_estado FROM tickets WHERE id_ticket = p_id_ticket FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El ticket % no existe', p_id_ticket;
    END IF;
    IF v_estado IN ('CERRADO', 'CANCELADO') THEN
        RAISE EXCEPTION 'No se puede asignar un ticket % ', v_estado;
    END IF;

    UPDATE tickets
       SET id_tecnico = p_id_tecnico,
           estado     = CASE WHEN estado = 'REGISTRADO' THEN 'EN_PROCESO' ELSE estado END
     WHERE id_ticket = p_id_ticket;   -- el trigger valida que sea técnico activo
END $$;

-- CU-09 / CU-16: cambiar estado respetando el flujo definido en el trigger.
CREATE OR REPLACE FUNCTION fn_cambiar_estado(p_id_ticket INTEGER, p_estado VARCHAR)
RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    UPDATE tickets SET estado = p_estado WHERE id_ticket = p_id_ticket;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El ticket % no existe', p_id_ticket;
    END IF;
END $$;

-- Asignación automática: técnico activo con menos tickets abiertos.
CREATE OR REPLACE FUNCTION fn_asignar_tecnico_auto(p_id_ticket INTEGER)
RETURNS INTEGER LANGUAGE plpgsql AS $$
DECLARE v_tec INTEGER;
BEGIN
    SELECT u.id_usuario INTO v_tec
      FROM usuarios u
      LEFT JOIN tickets t ON t.id_tecnico = u.id_usuario AND t.estado IN ('REGISTRADO', 'EN_PROCESO')
     WHERE u.rol = 'TECNICO' AND u.estado
     GROUP BY u.id_usuario
     ORDER BY COUNT(t.id_ticket), u.id_usuario
     LIMIT 1;
    IF v_tec IS NULL THEN
        RAISE EXCEPTION 'No hay técnicos activos disponibles';
    END IF;
    PERFORM fn_asignar_tecnico(p_id_ticket, v_tec);
    RETURN v_tec;
END $$;

-- Reporte por rango de fechas: tickets por categoría y estado.
CREATE OR REPLACE FUNCTION fn_reporte_tickets(p_desde DATE, p_hasta DATE)
RETURNS TABLE (categoria VARCHAR, total BIGINT, abiertos BIGINT, resueltos BIGINT, horas_prom_resolucion NUMERIC)
LANGUAGE sql STABLE AS $$
    SELECT c.nombre,
           COUNT(*),
           COUNT(*) FILTER (WHERE t.estado IN ('REGISTRADO', 'EN_PROCESO')),
           COUNT(*) FILTER (WHERE t.estado IN ('RESUELTO', 'CERRADO')),
           ROUND(AVG(EXTRACT(EPOCH FROM (t.fecha_resolucion - t.fecha_creacion)) / 3600)::numeric, 2)
      FROM tickets t JOIN categorias c ON c.id_categoria = t.id_categoria
     WHERE t.fecha_creacion >= p_desde AND t.fecha_creacion < p_hasta + 1
     GROUP BY c.nombre
     ORDER BY COUNT(*) DESC;
$$;

-- =============================================================================
-- 6. VISTAS
--    → ReporteService (JdbcTemplate) lee EXACTAMENTE estas tres vistas:
--        vw_kpi_general, vw_carga_tecnicos, vw_tickets_sin_asignar
-- =============================================================================

-- Ticket con nombres legibles y tiempo de resolución (para listados y reportes).
CREATE OR REPLACE VIEW vw_tickets_detalle AS
SELECT t.id_ticket, t.codigo, t.asunto, t.tipo_solicitud, t.prioridad, t.estado,
       c.nombre                                   AS categoria,
       s.nombres || ' ' || s.apellidos            AS solicitante,
       tc.nombres || ' ' || tc.apellidos          AS tecnico,
       t.fecha_creacion, t.fecha_resolucion,
       ROUND(EXTRACT(EPOCH FROM (t.fecha_resolucion - t.fecha_creacion)) / 3600, 2) AS horas_resolucion
  FROM tickets t
  JOIN categorias c  ON c.id_categoria = t.id_categoria
  JOIN usuarios   s  ON s.id_usuario   = t.id_solicitante
  LEFT JOIN usuarios tc ON tc.id_usuario = t.id_tecnico;

-- Tickets por estado y prioridad (dashboard del administrador).
CREATE OR REPLACE VIEW vw_resumen_estado_prioridad AS
SELECT estado, prioridad, COUNT(*) AS total
  FROM tickets GROUP BY estado, prioridad ORDER BY estado, prioridad;

-- Carga de trabajo por técnico (incluye técnicos sin tickets).
CREATE OR REPLACE VIEW vw_carga_tecnicos AS
SELECT u.id_usuario, u.nombres || ' ' || u.apellidos AS tecnico,
       COUNT(t.id_ticket) FILTER (WHERE t.estado IN ('REGISTRADO', 'EN_PROCESO')) AS abiertos,
       COUNT(t.id_ticket) FILTER (WHERE t.estado IN ('RESUELTO', 'CERRADO'))      AS atendidos
  FROM usuarios u LEFT JOIN tickets t ON t.id_tecnico = u.id_usuario
 WHERE u.rol = 'TECNICO' AND u.estado
 GROUP BY u.id_usuario, u.nombres, u.apellidos;

-- Tickets sin técnico, por prioridad y antigüedad (bandeja del administrador).
CREATE OR REPLACE VIEW vw_tickets_sin_asignar AS
SELECT id_ticket, codigo, asunto, prioridad, fecha_creacion
  FROM tickets
 WHERE id_tecnico IS NULL AND estado = 'REGISTRADO'
 ORDER BY CASE prioridad WHEN 'CRITICA' THEN 1 WHEN 'ALTA' THEN 2 WHEN 'MEDIA' THEN 3 ELSE 4 END,
          fecha_creacion;

-- KPIs generales (objetivo del proyecto: tiempo de resolución).
CREATE OR REPLACE VIEW vw_kpi_general AS
SELECT COUNT(*)                                                     AS total_tickets,
       COUNT(*) FILTER (WHERE estado IN ('REGISTRADO','EN_PROCESO')) AS abiertos,
       COUNT(*) FILTER (WHERE estado IN ('RESUELTO','CERRADO'))      AS resueltos,
       ROUND(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion)) / 3600)::numeric, 2) AS horas_prom_resolucion
  FROM tickets;

-- =============================================================================
-- 7. DATOS INICIALES
-- =============================================================================
INSERT INTO categorias (nombre, descripcion) VALUES
    ('Hardware',             'Problemas con equipos: PCs, laptops, impresoras, periféricos'),
    ('Software',             'Instalación, configuración y errores de aplicaciones'),
    ('Redes e Internet',     'Conectividad, Wi-Fi, VPN y acceso a la red institucional'),
    ('Cuentas y Accesos',    'Creación/recuperación de cuentas y permisos'),
    ('Correo Institucional', 'Incidencias con el correo institucional'),
    ('Aula Virtual',         'Soporte sobre la plataforma académica / LMS');

COMMIT;

-- =============================================================================
-- FIN DEL SCRIPT
-- =============================================================================
