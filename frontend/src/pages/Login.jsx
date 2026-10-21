import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { mensajeError } from '../services/api.js'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setCargando(true)
    try {
      await login(correo.trim(), password)
      const destino = location.state?.from?.pathname || '/tickets'
      navigate(destino, { replace: true })
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setCargando(false)
    }
  }

  function usarDemo(demoCorreo, demoPassword) {
    setCorreo(demoCorreo)
    setPassword(demoPassword)
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="brand-mark lg">ASM</span>
          <h1>Mesa de Ayuda</h1>
          <p>Instituto Cibertec · Soporte técnico</p>
        </div>

        <form onSubmit={onSubmit} className="form">
          <label>
            Correo institucional
            <input
              type="email"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              placeholder="usuario@cibertec.edu.pe"
              required
              autoComplete="username"
            />
          </label>

          <label>
            Contraseña
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              autoComplete="current-password"
            />
          </label>

          {error && <div className="alert alert-error">{error}</div>}

          <button className="btn btn-primary btn-block" disabled={cargando}>
            {cargando ? 'Ingresando…' : 'Iniciar sesión'}
          </button>
        </form>

        <p className="auth-alt">
          ¿No tienes cuenta? <Link to="/register">Regístrate</Link>
        </p>

        <div className="demo-box">
          <span>Cuentas de prueba</span>
          <div className="demo-grid">
            <button type="button" onClick={() => usarDemo('admin@cibertec.edu.pe', 'Admin123!')}>
              Admin
            </button>
            <button type="button" onClick={() => usarDemo('tecnico1@cibertec.edu.pe', 'Tecnico123!')}>
              Técnico
            </button>
            <button type="button" onClick={() => usarDemo('alumno@cibertec.edu.pe', 'Alumno123!')}>
              Estudiante
            </button>
            <button type="button" onClick={() => usarDemo('docente@cibertec.edu.pe', 'Docente123!')}>
              Docente
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
