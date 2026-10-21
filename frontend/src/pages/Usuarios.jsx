import { useEffect, useState } from 'react'
import api, { mensajeError } from '../services/api.js'

const ETIQUETA_ROL = {
  ESTUDIANTE: 'Estudiante',
  DOCENTE: 'Docente',
  TECNICO: 'Técnico',
  ADMINISTRADOR: 'Administrador',
}

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [buscar, setBuscar] = useState('')

  useEffect(() => {
    api
      .get('/usuarios')
      .then(({ data }) => setUsuarios(data))
      .catch((err) => setError(mensajeError(err)))
      .finally(() => setCargando(false))
  }, [])

  const filtrados = usuarios.filter((u) => {
    const texto = buscar.trim().toLowerCase()
    if (!texto) return true
    return (
      u.correo.toLowerCase().includes(texto) ||
      `${u.nombres} ${u.apellidos}`.toLowerCase().includes(texto)
    )
  })

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Usuarios</h1>
          <p className="muted">{usuarios.length} usuario(s) registrados</p>
        </div>
      </header>

      <div className="toolbar">
        <input
          className="search"
          placeholder="Buscar por nombre o correo…"
          value={buscar}
          onChange={(e) => setBuscar(e.target.value)}
        />
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {cargando ? (
        <div className="empty">Cargando usuarios…</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Nombre</th>
                <th>Correo</th>
                <th>Rol</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.map((u) => (
                <tr key={u.idUsuario}>
                  <td className="mono">{u.idUsuario}</td>
                  <td>
                    {u.nombres} {u.apellidos}
                  </td>
                  <td>{u.correo}</td>
                  <td>
                    <span className={`badge rol-${u.rol}`}>{ETIQUETA_ROL[u.rol] ?? u.rol}</span>
                  </td>
                  <td>
                    <span className={u.estado ? 'badge estado-CERRADO' : 'badge estado-CANCELADO'}>
                      {u.estado ? 'Activo' : 'Inactivo'}
                    </span>
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
