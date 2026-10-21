import { ETIQUETA_ESTADO, ETIQUETA_PRIORIDAD } from '../utils/format.js'

export function EstadoBadge({ estado }) {
  return (
    <span className={`badge estado-${estado}`}>
      {ETIQUETA_ESTADO[estado] ?? estado}
    </span>
  )
}

export function PrioridadBadge({ prioridad }) {
  return (
    <span className={`badge prioridad-${prioridad}`}>
      {ETIQUETA_PRIORIDAD[prioridad] ?? prioridad}
    </span>
  )
}
