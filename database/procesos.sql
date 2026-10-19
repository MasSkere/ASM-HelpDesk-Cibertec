-- =============================================================================
--  ASM-HelpDesk-Cibertec — Procesos, vistas y reportes (PostgreSQL 15+)
--  Ejecutar DESPUÉS de asm_helpdesk_postgres16.sql:
--    psql -h localhost -U helpdesk_admin -d helpdesk_db -f database/procesos.sql
--  Es re-ejecutable (CREATE OR REPLACE).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- PROCESOS (funciones)
-- -----------------------------------------------------------------------------

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

-- -----------------------------------------------------------------------------
-- VISTAS
-- -----------------------------------------------------------------------------

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
SELECT COUNT(*)                                                    AS total_tickets,
       COUNT(*) FILTER (WHERE estado IN ('REGISTRADO','EN_PROCESO')) AS abiertos,
       COUNT(*) FILTER (WHERE estado IN ('RESUELTO','CERRADO'))      AS resueltos,
       ROUND(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion)) / 3600)::numeric, 2) AS horas_prom_resolucion
  FROM tickets;
