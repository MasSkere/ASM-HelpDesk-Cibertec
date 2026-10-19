-- =============================================================================
--  ASM-HelpDesk-Cibertec — Esquema de Base de Datos (PostgreSQL 15+)
--  ---------------------------------------------------------------------------
--  Tablas (orden de creación según dependencias por FK):
--    1. usuarios    (sin dependencias)
--    2. categorias  (sin dependencias)
--    3. tickets     (depende de usuarios y categorias)
--    4. historial_ticket (depende de tickets; se llena sola por trigger)
--
--  Procesos, vistas y reportes: database/procesos.sql (ejecutar después)
--  Datos de prueba:             database/datos_prueba.sql (opcional)
--
--  Ejecución manual:
--    psql -h localhost -U helpdesk_admin -d helpdesk_db -f database/schema.sql
--
--  Docker: montar este archivo en /docker-entrypoint-initdb.d/ para que se
--  ejecute SOLO la primera vez que se crea el volumen de PostgreSQL.
--
--  ADVERTENCIA: el bloque DROP del inicio BORRA los datos existentes.
--  Úselo únicamente en desarrollo. En producción, elimine ese bloque.
-- =============================================================================

BEGIN;

-- Re-ejecutable en desarrollo
DROP TABLE IF EXISTS historial_ticket CASCADE;
DROP TABLE IF EXISTS tickets    CASCADE;
DROP TABLE IF EXISTS categorias CASCADE;
DROP TABLE IF EXISTS usuarios   CASCADE;
DROP FUNCTION IF EXISTS fn_tickets_validar()      CASCADE;
DROP FUNCTION IF EXISTS fn_tickets_historial()    CASCADE;

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

-- Unicidad del correo INSENSIBLE a mayúsculas (un UNIQUE simple distingue
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
    id_ticket          INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo             VARCHAR(20)  NOT NULL
                       DEFAULT ('TCK-' || to_char(CURRENT_DATE, 'YYYY') || '-'
                                || lpad(nextval('seq_tickets_codigo')::text, 4, '0')),
    asunto             VARCHAR(150) NOT NULL,
    descripcion        TEXT,
    tipo_solicitud     VARCHAR(30)  NOT NULL,
    prioridad          VARCHAR(20)  NOT NULL DEFAULT 'MEDIA',
    estado             VARCHAR(20)  NOT NULL DEFAULT 'REGISTRADO',
    fecha_creacion     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion   TIMESTAMP,                      -- se llena al pasar a RESUELTO (tiempo de resolución)
    id_solicitante     INTEGER      NOT NULL,          -- Usuario que reporta
    id_tecnico         INTEGER,                        -- Técnico asignado (NULL si aún no)
    id_categoria       INTEGER      NOT NULL,

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

COMMENT ON TABLE  tickets                     IS 'Tickets de soporte técnico (mesa de ayuda)';
COMMENT ON COLUMN tickets.codigo              IS 'Código legible único, ej. TCK-2026-0001 (se genera solo si no se envía)';
COMMENT ON COLUMN tickets.tipo_solicitud      IS 'INCIDENTE | REQUERIMIENTO | CONSULTA';
COMMENT ON COLUMN tickets.prioridad           IS 'BAJA | MEDIA | ALTA | CRITICA';
COMMENT ON COLUMN tickets.estado              IS 'REGISTRADO | EN_PROCESO | RESUELTO | CERRADO | CANCELADO';
COMMENT ON COLUMN tickets.id_tecnico          IS 'Técnico asignado; NULL mientras no se asigne';
COMMENT ON COLUMN tickets.fecha_resolucion    IS 'Momento en que pasó a RESUELTO; base para medir tiempo de resolución';

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
-- DATOS INICIALES
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
