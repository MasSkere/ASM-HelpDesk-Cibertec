export function formatFecha(valor) {
  if (!valor) return '—'
  const fecha = new Date(valor)
  if (Number.isNaN(fecha.getTime())) return valor
  return fecha.toLocaleString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export const ETIQUETA_ESTADO = {
  REGISTRADO: 'Registrado',
  EN_PROCESO: 'En proceso',
  RESUELTO: 'Resuelto',
  CERRADO: 'Cerrado',
  CANCELADO: 'Cancelado',
}

export const ETIQUETA_PRIORIDAD = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
}

export const ETIQUETA_TIPO = {
  INCIDENTE: 'Incidente',
  REQUERIMIENTO: 'Requerimiento',
  CONSULTA: 'Consulta',
}

/** Transiciones de estado válidas según la máquina de estados del backend. */
export function transicionesPermitidas(estado) {
  switch (estado) {
    case 'REGISTRADO':
      return ['EN_PROCESO', 'CANCELADO']
    case 'EN_PROCESO':
      return ['RESUELTO', 'CANCELADO']
    case 'RESUELTO':
      return ['CERRADO']
    default:
      return []
  }
}

export function puedeGestionarEstado(rol) {
  return rol === 'TECNICO' || rol === 'ADMINISTRADOR'
}
