import { useEffect, useRef, useState } from 'react'
import api, { mensajeError } from '../services/api.js'

const BIENVENIDA = {
  rol: 'bot',
  texto: '¡Hola! Soy el asistente IA de la Mesa de Ayuda ASM. Puedo orientarte para reportar una incidencia, saber cómo crear un ticket o consultar sus estados. ¿En qué te ayudo?',
  origen: null,
}

const ETIQUETA_ORIGEN = {
  WATSONX: 'IBM watsonx',
  LOCAL: 'Asistente local',
}

export default function Chatbot() {
  const [abierto, setAbierto] = useState(false)
  const [mensajes, setMensajes] = useState([BIENVENIDA])
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const finRef = useRef(null)

  useEffect(() => {
    if (abierto) finRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [mensajes, abierto])

  async function enviar(event) {
    event.preventDefault()
    const mensaje = texto.trim()
    if (!mensaje || enviando) return

    setMensajes((m) => [...m, { rol: 'user', texto: mensaje, origen: null }])
    setTexto('')
    setEnviando(true)

    try {
      const { data } = await api.post('/chatbot/chat', {
        mensaje,
        contexto: 'GENERAL',
      })
      setMensajes((m) => [
        ...m,
        { rol: 'bot', texto: data.respuesta, origen: data.origen },
      ])
    } catch (err) {
      setMensajes((m) => [
        ...m,
        {
          rol: 'bot',
          texto: `Ocurrió un problema: ${mensajeError(err)}`,
          origen: null,
        },
      ])
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="chatbot">
      {abierto && (
        <div className="chatbot-panel" role="dialog" aria-label="Asistente IA">
          <header className="chatbot-header">
            <div>
              <strong>Asistente IA</strong>
              <small>Mesa de Ayuda ASM</small>
            </div>
            <button
              type="button"
              className="chatbot-close"
              onClick={() => setAbierto(false)}
              aria-label="Cerrar chat"
            >
              ✕
            </button>
          </header>

          <div className="chatbot-mensajes">
            {mensajes.map((m, i) => (
              <div key={i} className={`chatbot-msg ${m.rol}`}>
                <p>{m.texto}</p>
                {m.origen && (
                  <span className="chatbot-origen">
                    {ETIQUETA_ORIGEN[m.origen] ?? m.origen}
                  </span>
                )}
              </div>
            ))}
            {enviando && (
              <div className="chatbot-msg bot">
                <p className="chatbot-typing">Escribiendo…</p>
              </div>
            )}
            <div ref={finRef} />
          </div>

          <form className="chatbot-form" onSubmit={enviar}>
            <input
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              placeholder="Escribe tu consulta…"
              maxLength={1000}
              autoFocus={abierto}
            />
            <button
              className="btn btn-primary"
              disabled={enviando || !texto.trim()}
            >
              Enviar
            </button>
          </form>
        </div>
      )}

      <button
        type="button"
        className="chatbot-fab"
        onClick={() => setAbierto((v) => !v)}
        aria-label={abierto ? 'Cerrar asistente' : 'Abrir asistente IA'}
        title="Asistente IA"
      >
        {abierto ? '✕' : '🤖'}
      </button>
    </div>
  )
}
