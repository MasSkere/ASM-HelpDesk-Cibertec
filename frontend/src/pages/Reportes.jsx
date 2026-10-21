import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { mensajeError } from '../services/api.js'
import { PrioridadBadge } from '../components/Badges.jsx'
import { formatFecha } from '../utils/format.js'

export default function Reportes() {
  const [kpi, setKpi] = useState(null)
  const [carga, setCarga] = useState([])
  const [sinAsignar, setSinAsignar] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([
      api.get('/reportes/kpi'),
      api.get('/reportes/carga-tecnicos'),
      api.get('/reportes/sin-asignar'),
    ])
      .then(([kpiRes, cargaRes, sinRes]) => {
        setKpi(kpiRes.data)
        setCarga(cargaRes.data)
        setSinAsignar(sinRes.data)
      })
      .catch((err) => setError(mensajeError(err)))
      .finally(() => setCargando(false))
  }, [])

  if (cargando) return <div className="page"><div className="empty">Cargando reportes…</div></div>

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Reportes</h1>
          <p className="muted">Indicadores de la mesa de ayuda</p>
        </div>
      </header>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="kpi-grid">
        <article className="kpi-card">
          <span>Total de tickets</span>
          <strong>{kpi?.totalTickets ?? 0}</strong>
        </article>
        <article className="kpi-card">
          <span>Abiertos</span>
          <strong className="text-warn">{kpi?.abiertos ?? 0}</strong>
        </article>
        <article className="kpi-card">
          <span>Resueltos</span>
          <strong className="text-ok">{kpi?.resueltos ?? 0}</strong>
        </article>
        <article className="kpi-card">
          <span>Horas prom. de resolución</span>
          <strong>{kpi?.horasPromResolucion ?? '—'}</strong>
        </article>
      </div>

      <section className="card">
        <h2>Carga por técnico</h2>
        {carga.length === 0 ? (
          <p className="muted">Sin datos.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Técnico</th>
                  <th>Abiertos</th>
                  <th>Atendidos</th>
                </tr>
              </thead>
              <tbody>
                {carga.map((t) => (
                  <tr key={t.idUsuario}>
                    <td>{t.tecnico}</td>
                    <td>{t.abiertos}</td>
                    <td>{t.atendidos}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="card">
        <h2>Tickets sin asignar</h2>
        {sinAsignar.length === 0 ? (
          <p className="muted">No hay tickets pendientes de asignación.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Asunto</th>
                  <th>Prioridad</th>
                  <th>Creado</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {sinAsignar.map((t) => (
                  <tr key={t.idTicket}>
                    <td className="mono">{t.codigo}</td>
                    <td>{t.asunto}</td>
                    <td>
                      <PrioridadBadge prioridad={t.prioridad} />
                    </td>
                    <td className="muted">{formatFecha(t.fechaCreacion)}</td>
                    <td className="right">
                      <Link className="link" to={`/tickets/${t.idTicket}`}>
                        Atender
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
