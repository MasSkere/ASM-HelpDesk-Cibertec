package com.cibertec.helpdesk.service;

import com.cibertec.helpdesk.dto.response.KpiGeneralResponse;
import com.cibertec.helpdesk.dto.response.CargaTecnicoResponse;
import com.cibertec.helpdesk.dto.response.TicketSinAsignarResponse;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {

    private final JdbcTemplate jdbcTemplate;

    public ReporteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public KpiGeneralResponse obtenerKpiGeneral() {

        String sql = """
                SELECT
                    total_tickets,
                    abiertos,
                    resueltos,
                    horas_prom_resolucion
                FROM vw_kpi_general
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new KpiGeneralResponse(
                rs.getLong("total_tickets"),
                rs.getLong("abiertos"),
                rs.getLong("resueltos"),
                rs.getBigDecimal("horas_prom_resolucion")));
    }

    @Transactional(readOnly = true)
    public List<CargaTecnicoResponse> obtenerCargaTecnicos() {

        String sql = """
                SELECT id_usuario, tecnico, abiertos, atendidos
                FROM vw_carga_tecnicos
                ORDER BY id_usuario
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new CargaTecnicoResponse(
                rs.getInt("id_usuario"),
                rs.getString("tecnico"),
                rs.getLong("abiertos"),
                rs.getLong("atendidos")));
    }

    @Transactional(readOnly = true)
    public List<TicketSinAsignarResponse> obtenerTicketsSinAsignar() {

        String sql = """
                SELECT id_ticket, codigo, asunto,
                       prioridad, fecha_creacion
                FROM vw_tickets_sin_asignar
                ORDER BY fecha_creacion
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TicketSinAsignarResponse(
                rs.getInt("id_ticket"),
                rs.getString("codigo"),
                rs.getString("asunto"),
                rs.getString("prioridad"),
                rs.getTimestamp("fecha_creacion")
                        .toLocalDateTime()));
    }

}
