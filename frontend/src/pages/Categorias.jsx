import { useEffect, useState } from 'react'
import api, { mensajeError } from '../services/api.js'

const VACIO = { nombre: '', descripcion: '' }

export default function Categorias() {
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState(VACIO)
  const [editandoId, setEditandoId] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  async function cargar() {
    setCargando(true)
    try {
      const { data } = await api.get('/categorias')
      setCategorias(data)
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargar()
  }, [])

  function limpiar() {
    setForm(VACIO)
    setEditandoId(null)
  }

  function editar(categoria) {
    setEditandoId(categoria.idCategoria)
    setForm({ nombre: categoria.nombre, descripcion: categoria.descripcion ?? '' })
    setError('')
    setAviso('')
  }

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setAviso('')
    setGuardando(true)
    try {
      if (editandoId) {
        await api.put(`/categorias/${editandoId}`, form)
        setAviso('Categoría actualizada')
      } else {
        await api.post('/categorias', form)
        setAviso('Categoría creada')
      }
      limpiar()
      await cargar()
    } catch (err) {
      setError(mensajeError(err))
    } finally {
      setGuardando(false)
    }
  }

  async function eliminar(categoria) {
    if (!window.confirm(`¿Eliminar la categoría "${categoria.nombre}"?`)) return
    setError('')
    setAviso('')
    try {
      await api.delete(`/categorias/${categoria.idCategoria}`)
      setAviso('Categoría eliminada')
      if (editandoId === categoria.idCategoria) limpiar()
      await cargar()
    } catch (err) {
      setError(mensajeError(err))
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Categorías</h1>
          <p className="muted">{categorias.length} categoría(s)</p>
        </div>
      </header>

      {error && <div className="alert alert-error">{error}</div>}
      {aviso && <div className="alert alert-ok">{aviso}</div>}

      <div className="detail-grid">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>#</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr>
                  <td colSpan={4} className="empty">
                    Cargando…
                  </td>
                </tr>
              ) : (
                categorias.map((c) => (
                  <tr key={c.idCategoria}>
                    <td className="mono">{c.idCategoria}</td>
                    <td>{c.nombre}</td>
                    <td className="muted">{c.descripcion || '—'}</td>
                    <td className="right nowrap">
                      <button className="btn btn-link" onClick={() => editar(c)}>
                        Editar
                      </button>
                      <button className="btn btn-link danger" onClick={() => eliminar(c)}>
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <aside className="side-col">
          <section className="card">
            <h2>{editandoId ? 'Editar categoría' : 'Nueva categoría'}</h2>
            <form className="form" onSubmit={onSubmit}>
              <label>
                Nombre
                <input
                  value={form.nombre}
                  onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                  maxLength={100}
                  required
                />
              </label>
              <label>
                Descripción
                <textarea
                  value={form.descripcion}
                  onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
                  rows={3}
                />
              </label>
              <div className="actions">
                {editandoId && (
                  <button type="button" className="btn btn-ghost" onClick={limpiar}>
                    Cancelar
                  </button>
                )}
                <button className="btn btn-primary" disabled={guardando}>
                  {guardando ? 'Guardando…' : editandoId ? 'Actualizar' : 'Crear'}
                </button>
              </div>
            </form>
          </section>
        </aside>
      </div>
    </div>
  )
}
