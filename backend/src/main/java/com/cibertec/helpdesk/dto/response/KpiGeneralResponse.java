package com.cibertec.helpdesk.dto.response;

import java.math.BigDecimal;

public record KpiGeneralResponse(
        Long totalTickets,
        Long abiertos,
        Long resueltos,
        BigDecimal horasPromResolucion
) {}
