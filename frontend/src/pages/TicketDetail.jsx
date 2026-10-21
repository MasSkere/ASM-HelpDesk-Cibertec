import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api, { mensajeError } from '../services/api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { EstadoBadge, PrioridadBadge } from '../components/Badges.jsx'
import {
  ETIQUETA_ESTADO,
  ETIQUETA_PRIORIDAD,
  ETIQUETA_TIPO,
  formatFecha,
  puedeGestionarEstado,
  transicionesPermitidas,
} from '../utils/format.js'

const ETIQUETA_ACCION = {
  CREADO: 'Ticket creado',
  CAMBIO_ESTADO: 'Cambio de estado',
  ASIGNACION: 'Asignación de técnico',
}

export default function TicketDetail() {
  const { id } = useParams()
  const { rol } = useAuth()

  const [ticket, setTicket] = useState(null)
  const [historial, setHistorial] = useState([])
  const [tecnicos, setTecnicos] = useState([])
  const [tecnicoSel, setTecnicoSel] = useState('')
  const [cargando, setCargando] = useState(true)
  const [procesando, setProcesando] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const [ticketRes, historialRes] = await Promise.all([
        api.get(`/tickets/${id}`),
        api.get(`/tickets/${id}/historial`),
      ])
      setTicket(ticketRes.data)
      setHistorial(historialRes.data)
      setTecnicoSel(ticketRes.data.idTecnico ?? '')
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setCargando(false)
    }
  }, [id])

  useEffect(() => {
    cargar()
  }, [cargar])

  useEffect(() => {
    if (rol !== 'ADMINISTRADOR') return
    api
      .get('/usuarios')
      .then(({ data }) => setTecnicos(data.filter((u) => u.rol === 'TECNICO' && u.estado)))
      .catch(() => {
        /* silencioso: solo es una ayuda para asignar */
      })
  }, [rol])

  async function cambiarEstado(nuevoEstado) {
    setProcesando(true)
    setError('')
    setAviso('')
    try {
      const { data } = await api.patch(`/tickets/${id}/estado`, { estado: nuevoEstado })
      setTicket(data)
      setAviso(`Estado actualizado a ${ETIQUETA_ESTADO[data.estado]}`)
      const { data: hist } = await api.get(`/tickets/${id}/historial`)
      setHistorial(hist)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setProcesando(false)
    }
  }

  async function asignar() {
    if (!tecnicoSel) return
    setProcesando(true)
    setError('')
    setAviso('')
    try {
      const { data } = await api.patch(`/tickets/${id}/asignar`, {
        idTecnico: Number(tecnicoSel),
      })
      setTicket(data)
      setAviso('Técnico asignado correctamente')
      const { data: hist } = await api.get(`/tickets/${id}/historial`)
      setHistorial(hist)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setProcesando(false)
    }
  }

  if (cargando) return <div className="page"><div className="empty">Cargando ticket…</div></div>
  if (error && !ticket) return <div className="page"><div className="alert alert-error">{error}</div></div>
  if (!ticket) return null

  const transiciones = transicionesPermitidas(ticket.estado)
  const gestiona = puedeGestionarEstado(rol)

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="breadcrumb">
            <Link to="/tickets">Tickets</Link> / <span className="mono">{ticket.codigo}</span>
          </p>
          <h1>{ticket.asunto}</h1>
          <p className="muted">
            <EstadoBadge estado={ticket.estado} /> <PrioridadBadge prioridad={ticket.prioridad} />
          </p>
        </div>
      </header>

      {error && <div className="alert alert-error">{error}</div>}
      {aviso && <div className="alert alert-ok">{aviso}</div>}

      <div className="detail-grid">
        <section className="card">
          <h2>Descripción</h2>
          <p className="descripcion">{ticket.descripcion || 'Sin descripción.'}</p>

          <dl className="meta">
            <div>
              <dt>Tipo</dt>
              <dd>{ETIQUETA_TIPO[ticket.tipoSolicitud] ?? ticket.tipoSolicitud}</dd>
            </div>
            <div>
              <dt>Prioridad</dt>
              <dd>{ETIQUETA_PRIORIDAD[ticket.prioridad] ?? ticket.prioridad}</dd>
            </div>
            <div>
              <dt>Estado</dt>
              <dd>{ETIQUETA_ESTADO[ticket.estado] ?? ticket.estado}</dd>
            </div>
            <div>
              <dt>Creado</dt>
              <dd>{formatFecha(ticket.fechaCreacion)}</dd>
            </div>
            <div>
              <dt>Solicitante</dt>
              <dd>Usuario #{ticket.idSolicitante}</dd>
            </div>
            <div>
              <dt>Técnico</dt>
              <dd>{ticket.idTecnico ? `Usuario #${ticket.idTecnico}` : 'Sin asignar'}</dd>
            </div>
            <div>
              <dt>Categoría</dt>
              <dd>Categoría #{ticket.idCategoria}</dd>
            </div>
          </dl>
        </section>

        <aside className="side-col">
          {gestiona && (
            <section className="card">
              <h2>Cambiar estado</h2>
              {transiciones.length === 0 ? (
                <p className="muted">Este ticket está en un estado final.</p>
              ) : (
                <div className="stack">
                  {transiciones.map((estado) => (
                    <button
                      key={estado}
                      className="btn btn-secondary"
                      disabled={procesando}
                      onClick={() => cambiarEstado(estado)}
                    >
                      Marcar como {ETIQUETA_ESTADO[estado]}
                    </button>
                  ))}
                </div>
              )}
            </section>
          )}

          {rol === 'ADMINISTRADOR' && (
            <section className="card">
              <h2>Asignar técnico</h2>
              <div className="stack">
                <select value={tecnicoSel} onChange={(e) => setTecnicoSel(e.target.value)}>
                  <option value="">Selecciona un técnico…</option>
                  {tecnicos.map((t) => (
                    <option key={t.idUsuario} value={t.idUsuario}>
                      {t.nombres} {t.apellidos}
                    </option>
                  ))}
                </select>
                <button
                  className="btn btn-primary"
                  disabled={procesando || !tecnicoSel}
                  onClick={asignar}
                >
                  Guardar asignación
                </button>
              </div>
            </section>
          )}
        </aside>
      </div>

      <section className="card">
        <h2>Historial</h2>
        {historial.length === 0 ? (
          <p className="muted">Sin movimientos registrados.</p>
        ) : (
          <ol className="timeline">
            {historial.map((h) => (
              <li key={h.idHistorial}>
                <span className="dot" />
                <div>
                  <strong>{ETIQUETA_ACCION[h.accion] ?? h.accion}</strong>
                  <p className="muted">
                    {h.estadoAnterior && h.estadoNuevo
                      ? `${ETIQUETA_ESTADO[h.estadoAnterior] ?? h.estadoAnterior} → ${
                          ETIQUETA_ESTADO[h.estadoNuevo] ?? h.estadoNuevo
                        }`
                      : 'Registro inicial'}
                    {' · '}
                    {formatFecha(h.fecha)}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  )
}
