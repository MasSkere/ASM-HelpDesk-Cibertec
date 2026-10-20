package com.cibertec.helpdesk.controller;

import com.cibertec.helpdesk.dto.response.KpiGeneralResponse;
import com.cibertec.helpdesk.service.ReporteService;
import com.cibertec.helpdesk.dto.response.CargaTecnicoResponse;
import com.cibertec.helpdesk.dto.response.TicketSinAsignarResponse;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/kpi")
    public ResponseEntity<KpiGeneralResponse> obtenerKpi() {
        return ResponseEntity.ok(
                reporteService.obtenerKpiGeneral());
    }

    @GetMapping("/carga-tecnicos")
    public ResponseEntity<List<CargaTecnicoResponse>> cargaTecnicos() {
        return ResponseEntity.ok(
                reporteService.obtenerCargaTecnicos());
    }

    @GetMapping("/sin-asignar")
    public ResponseEntity<List<TicketSinAsignarResponse>> ticketsSinAsignar() {
        return ResponseEntity.ok(
                reporteService.obtenerTicketsSinAsignar());
    }

}
