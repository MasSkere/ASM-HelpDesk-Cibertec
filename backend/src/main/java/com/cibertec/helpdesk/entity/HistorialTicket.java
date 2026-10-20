package com.cibertec.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_ticket")
@Getter
@Setter
@NoArgsConstructor
public class HistorialTicket {

    @Id
    @Column(name = "id_historial")
    private Integer idHistorial;

    @Column(name = "id_ticket", nullable = false)
    private Integer idTicket;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 20)
    private String estadoNuevo;

    @Column(name = "id_tecnico")
    private Integer idTecnico;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
