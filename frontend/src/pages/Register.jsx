import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { mensajeError } from '../services/api.js'

const VACIO = { nombres: '', apellidos: '', correo: '', password: '' }

export default function Register() {
  const { registrar } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(VACIO)
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  function onChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setCargando(true)
    try {
      await registrar(form)
      navigate('/tickets', { replace: true })
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="brand-mark lg">ASM</span>
          <h1>Crear cuenta</h1>
          <p>Se creará con rol Estudiante</p>
        </div>

        <form onSubmit={onSubmit} className="form">
          <div className="field-row">
            <label>
              Nombres
              <input name="nombres" value={form.nombres} onChange={onChange} required maxLength={100} />
            </label>
            <label>
              Apellidos
              <input name="apellidos" value={form.apellidos} onChange={onChange} required maxLength={100} />
            </label>
          </div>

          <label>
            Correo institucional
            <input
              name="correo"
              type="email"
              value={form.correo}
              onChange={onChange}
              placeholder="usuario@cibertec.edu.pe"
              required
            />
          </label>

          <label>
            Contraseña
            <input
              name="password"
              type="password"
              value={form.password}
              onChange={onChange}
              minLength={8}
              required
              autoComplete="new-password"
            />
          </label>

          {error && <div className="alert alert-error">{error}</div>}

          <button className="btn btn-primary btn-block" disabled={cargando}>
            {cargando ? 'Creando cuenta…' : 'Registrarme'}
          </button>
        </form>

        <p className="auth-alt">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </div>
  )
}
