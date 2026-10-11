package com.cibertec.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla {@code tickets}.
 * Solo atributos básicos (las FK se representan como {@code Long},
 * sin anotaciones de relación todavía).
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Long idTicket;

    /** Código legible único, ej. "TCK-2026-0001". */
    @Column(name = "codigo", nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(name = "asunto", nullable = false, length = 150)
    private String asunto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitud", nullable = false, length = 30)
    private TipoSolicitud tipoSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTicket estado;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // ------------------------------------------------------------------
    // FK sin relación JPA (hasta que se implementen las relaciones)
    // ------------------------------------------------------------------
    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;

    @Column(name = "id_tecnico")
    private Long idTecnico;

    @Column(name = "id_categoria", nullable = false)
    private Long idCategoria;
}