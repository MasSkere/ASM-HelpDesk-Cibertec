import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { mensajeError } from '../services/api.js'
import { ETIQUETA_PRIORIDAD, ETIQUETA_TIPO } from '../utils/format.js'

const VACIO = {
  asunto: '',
  descripcion: '',
  tipoSolicitud: 'INCIDENTE',
  prioridad: 'MEDIA',
  idCategoria: '',
}

export default function NuevoTicket() {
  const navigate = useNavigate()
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(VACIO)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [sugiriendo, setSugiriendo] = useState(false)
  const [sugerencia, setSugerencia] = useState(null)
  const [errorSugerencia, setErrorSugerencia] = useState('')

  useEffect(() => {
    api
      .get('/categorias')
      .then(({ data }) => setCategorias(data))
      .catch((err) => setError(mensajeError(err)))
  }, [])

  function onChange(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  // Pide al asistente IA que clasifique el ticket y aplica la sugerencia.
  async function sugerirConIA() {
    setErrorSugerencia('')
    setSugerencia(null)

    if (!form.asunto.trim() || !form.descripcion.trim()) {
      setErrorSugerencia(
        'Escribe el asunto y la descripción antes de pedir la sugerencia con IA.',
      )
      return
    }

    setSugiriendo(true)
    try {
      const { data } = await api.post('/chatbot/sugerir-ticket', {
        asunto: form.asunto,
        descripcion: form.descripcion,
      })
      setSugerencia(data)
      setForm((f) => ({
        ...f,
        tipoSolicitud: data.tipoSolicitud,
        prioridad: data.prioridad,
        idCategoria: data.idCategoria ? String(data.idCategoria) : f.idCategoria,
      }))
    } catch (err) {
      setErrorSugerencia(mensajeError(err))
    } finally {
      setSugiriendo(false)
    }
  }

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setEnviando(true)
    try {
      const { data } = await api.post('/tickets', {
        ...form,
        idCategoria: Number(form.idCategoria),
      })
      navigate(`/tickets/${data.idTicket}`, { replace: true })
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Nuevo ticket</h1>
          <p className="muted">Describe tu incidencia o requerimiento</p>
        </div>
      </header>

      <form className="card form" onSubmit={onSubmit}>
        <label>
          Asunto
          <input
            name="asunto"
            value={form.asunto}
            onChange={onChange}
            maxLength={150}
            required
            placeholder="Ej. No hay conexión Wi-Fi en el laboratorio 3"
          />
        </label>

        <label>
          Descripción
          <textarea
            name="descripcion"
            value={form.descripcion}
            onChange={onChange}
            rows={5}
            required
            placeholder="Detalla el problema con el mayor contexto posible…"
          />
        </label>

        <div className="sugerencia-ia-fila">
          <button
            type="button"
            className="btn btn-ghost"
            onClick={sugerirConIA}
            disabled={sugiriendo}
            title="Usa el asistente IA (IBM watsonx) para clasificar el ticket"
          >
            {sugiriendo ? 'Analizando…' : '✨ Sugerir con IA'}
          </button>
          {errorSugerencia && (
            <span className="sugerencia-ia-error">{errorSugerencia}</span>
          )}
        </div>

        {sugerencia && (
          <div className="alert alert-ok">
            <strong>Sugerencia aplicada:</strong> {sugerencia.tipoSolicitud} ·{' '}
            {sugerencia.prioridad} · {sugerencia.categoria}.{' '}
            {sugerencia.justificacion}
            <small className="muted">
              {' '}
              (origen: {sugerencia.origen === 'WATSONX' ? 'IBM watsonx' : 'asistente local'})
            </small>
          </div>
        )}

        <div className="field-row">
          <label>
            Tipo de solicitud
            <select name="tipoSolicitud" value={form.tipoSolicitud} onChange={onChange}>
              {Object.entries(ETIQUETA_TIPO).map(([valor, etiqueta]) => (
                <option key={valor} value={valor}>
                  {etiqueta}
                </option>
              ))}
            </select>
          </label>

          <label>
            Prioridad
            <select name="prioridad" value={form.prioridad} onChange={onChange}>
              {Object.entries(ETIQUETA_PRIORIDAD).map(([valor, etiqueta]) => (
                <option key={valor} value={valor}>
                  {etiqueta}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label>
          Categoría
          <select name="idCategoria" value={form.idCategoria} onChange={onChange} required>
            <option value="">Selecciona una categoría…</option>
            {categorias.map((c) => (
              <option key={c.idCategoria} value={c.idCategoria}>
                {c.nombre}
              </option>
            ))}
          </select>
        </label>

        {error && <div className="alert alert-error">{error}</div>}

        <div className="actions">
          <button type="button" className="btn btn-ghost" onClick={() => navigate(-1)}>
            Cancelar
          </button>
          <button className="btn btn-primary" disabled={enviando}>
            {enviando ? 'Enviando…' : 'Registrar ticket'}
          </button>
        </div>
      </form>
    </div>
  )
}
