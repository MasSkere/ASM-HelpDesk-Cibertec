import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../services/api.js'
import { useAuth } from '../context/AuthContext.jsx'
import { EstadoBadge, PrioridadBadge } from '../components/Badges.jsx'
import { ETIQUETA_ESTADO, formatFecha } from '../utils/format.js'

const TITULO_POR_ROL = {
  ESTUDIANTE: 'Mis tickets',
  DOCENTE: 'Mis tickets',
  TECNICO: 'Tickets asignados',
  ADMINISTRADOR: 'Todos los tickets',
}

export default function Tickets() {
  const { rol } = useAuth()
  const [tickets, setTickets] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [estado, setEstado] = useState('TODOS')
  const [buscar, setBuscar] = useState('')

  useEffect(() => {
    let activo = true
    api
      .get('/tickets')
      .then(({ data }) => activo && setTickets(data))
      .catch((err) => activo && setError(mensajeError(err)))
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [])

  const filtrados = useMemo(() => {
    const texto = buscar.trim().toLowerCase()
    return tickets.filter((t) => {
      const coincideEstado = estado === 'TODOS' || t.estado === estado
      const coincideTexto =
        !texto ||
        (t.codigo ?? '').toLowerCase().includes(texto) ||
        (t.asunto ?? '').toLowerCase().includes(texto)
      return coincideEstado && coincideTexto
    })
  }, [tickets, estado, buscar])

  const puedeCrear = rol === 'ESTUDIANTE' || rol === 'DOCENTE'

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{TITULO_POR_ROL[rol] ?? 'Tickets'}</h1>
          <p className="muted">{tickets.length} ticket(s) en total</p>
        </div>
        {puedeCrear && (
          <Link className="btn btn-primary" to="/tickets/nuevo">
            + Nuevo ticket
          </Link>
        )}
      </header>

      <div className="toolbar">
        <input
          className="search"
          placeholder="Buscar por código o asunto…"
          value={buscar}
          onChange={(e) => setBuscar(e.target.value)}
        />
        <select value={estado} onChange={(e) => setEstado(e.target.value)}>
          <option value="TODOS">Todos los estados</option>
          {Object.entries(ETIQUETA_ESTADO).map(([valor, etiqueta]) => (
            <option key={valor} value={valor}>
              {etiqueta}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {cargando ? (
        <div className="empty">Cargando tickets…</div>
      ) : filtrados.length === 0 ? (
        <div className="empty">No hay tickets que coincidan con el filtro.</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Código</th>
                <th>Asunto</th>
                <th>Prioridad</th>
                <th>Estado</th>
                <th>Creado</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {filtrados.map((t) => (
                <tr key={t.idTicket}>
                  <td className="mono">{t.codigo}</td>
                  <td>{t.asunto}</td>
                  <td>
                    <PrioridadBadge prioridad={t.prioridad} />
                  </td>
                  <td>
                    <EstadoBadge estado={t.estado} />
                  </td>
                  <td className="muted">{formatFecha(t.fechaCreacion)}</td>
                  <td className="right">
                    <Link className="link" to={`/tickets/${t.idTicket}`}>
                      Ver
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
