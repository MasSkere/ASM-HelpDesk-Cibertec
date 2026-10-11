-- =============================================================================
--  ASM-HelpDesk-Cibertec — Esquema de Base de Datos (PostgreSQL 15+)
--  ---------------------------------------------------------------------------
--  Orden de creación (respetando dependencias por FK):
--    1. usuarios  (entidad base, sin dependencias)
--    2. categorias (entidad base, sin dependencias)
--    3. tickets   (depende de usuarios y categorias)
--
--  Ejecución manual:
--    psql -h localhost -U helpdesk_admin -d helpdesk_db -f database/schema.sql
--
--  Nota: este script se ejecuta automáticamente la primera vez que se
--  inicializa el volumen de PostgreSQL (ver docker-compose.yml).
-- =============================================================================

-- Idempotencia básica: permite re-ejecutar el script sin errores de "ya existe".
DROP TABLE IF EXISTS tickets     CASCADE;
DROP TABLE IF EXISTS categorias  CASCADE;
DROP TABLE IF EXISTS usuarios    CASCADE;

-- =============================================================================
-- 1. USUARIOS
-- =============================================================================
CREATE TABLE usuarios (
    id_usuario  SERIAL       PRIMARY KEY,
    nombres     VARCHAR(100) NOT NULL,
    apellidos   VARCHAR(100) NOT NULL,
    correo      VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,         -- Hash BCrypt (NUNCA texto plano)
    rol         VARCHAR(20)  NOT NULL,
    estado      BOOLEAN      NOT NULL DEFAULT TRUE,  -- TRUE = activo, FALSE = inactivo

    -- Unicidad insensible a mayúsculas del correo
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),

    -- Roles permitidos en el sistema
    CONSTRAINT chk_usuarios_rol
        CHECK (rol IN ('ESTUDIANTE', 'DOCENTE', 'TECNICO', 'ADMINISTRADOR'))
);

COMMENT ON TABLE  usuarios           IS 'Usuarios del sistema (solicitantes, técnicos y administradores)';
COMMENT ON COLUMN usuarios.password  IS 'Contraseña cifrada con BCrypt';
COMMENT ON COLUMN usuarios.rol       IS 'ESTUDIANTE | DOCENTE | TECNICO | ADMINISTRADOR';
COMMENT ON COLUMN usuarios.estado    IS 'TRUE = activo, FALSE = inactivo';

CREATE INDEX idx_usuarios_rol    ON usuarios (rol);
CREATE INDEX idx_usuarios_estado ON usuarios (estado);

-- =============================================================================
-- 2. CATEGORIAS
-- =============================================================================
CREATE TABLE categorias (
    id_categoria SERIAL       PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  TEXT,

    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
);

COMMENT ON TABLE categorias IS 'Categorías/clasificación de los tickets (ej. Hardware, Software, Redes, Cuentas)';

-- =============================================================================
-- 3. TICKETS
-- =============================================================================
CREATE TABLE tickets (
    id_ticket       SERIAL       PRIMARY KEY,
    codigo          VARCHAR(20)  NOT NULL,       -- Código legible: "TCK-2026-0001"
    asunto          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    tipo_solicitud  VARCHAR(30)  NOT NULL,
    prioridad       VARCHAR(20)  NOT NULL DEFAULT 'MEDIA',
    estado          VARCHAR(20)  NOT NULL DEFAULT 'REGISTRADO',
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_solicitante  INTEGER      NOT NULL,        -- Usuario que reporta (Estudiante/Docente)
    id_tecnico      INTEGER,                      -- Técnico asignado (NULL si aún no se asigna)
    id_categoria    INTEGER      NOT NULL,        -- Categoría del ticket

    -- Código de ticket único
    CONSTRAINT uq_tickets_codigo UNIQUE (codigo),

    -- Dominios de valores controlados
    CONSTRAINT chk_tickets_tipo
        CHECK (tipo_solicitud IN ('INCIDENTE', 'REQUERIMIENTO', 'CONSULTA')),
    CONSTRAINT chk_tickets_prioridad
        CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT chk_tickets_estado
        CHECK (estado IN ('REGISTRADO', 'EN_PROCESO', 'RESUELTO', 'CERRADO', 'CANCELADO')),

    -- Relaciones (Foreign Keys)
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

COMMENT ON TABLE  tickets                 IS 'Tickets de soporte técnico (mesa de ayuda)';
COMMENT ON COLUMN tickets.tipo_solicitud  IS 'INCIDENTE | REQUERIMIENTO | CONSULTA';
COMMENT ON COLUMN tickets.prioridad       IS 'BAJA | MEDIA | ALTA | CRITICA';
COMMENT ON COLUMN tickets.estado          IS 'REGISTRADO | EN_PROCESO | RESUELTO | CERRADO | CANCELADO';
COMMENT ON COLUMN tickets.id_tecnico      IS 'Técnico asignado; NULL mientras no se asigne';

-- Índices para las consultas más frecuentes
CREATE INDEX idx_tickets_estado        ON tickets (estado);
CREATE INDEX idx_tickets_prioridad     ON tickets (prioridad);
CREATE INDEX idx_tickets_solicitante   ON tickets (id_solicitante);
CREATE INDEX idx_tickets_tecnico       ON tickets (id_tecnico);
CREATE INDEX idx_tickets_categoria     ON tickets (id_categoria);
CREATE INDEX idx_tickets_fecha_creacion ON tickets (fecha_creacion DESC);

-- =============================================================================
-- DATOS INICIALES (catálogo de categorías de ejemplo)
-- =============================================================================
INSERT INTO categorias (nombre, descripcion) VALUES
    ('Hardware',        'Problemas con equipos: PCs, laptops, impresoras, periféricos'),
    ('Software',        'Instalación, configuración y errores de aplicaciones'),
    ('Redes e Internet', 'Conectividad, Wi-Fi, VPN y acceso a la red institucional'),
    ('Cuentas y Accesos','Creación/recuperación de cuentas y permisos'),
    ('Correo Institucional', 'Incidencias con el correo institucional'),
    ('Aula Virtual',    'Soporte sobre la plataforma académica / LMS');

-- =============================================================================
-- FIN DEL SCRIPT
-- =============================================================================
