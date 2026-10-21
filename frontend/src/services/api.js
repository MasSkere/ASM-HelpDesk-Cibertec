import axios from 'axios'

// En dev se usa el proxy de Vite ('/api'); en Docker se inyecta la URL pública.
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

// Adjunta el token JWT en cada petición.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('asm_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Si el token expira o es inválido, limpia la sesión.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('asm_token')
      localStorage.removeItem('asm_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

// Mensaje de error legible a partir del formato estándar del backend.
export function mensajeError(error) {
  const data = error?.response?.data
  if (data?.detalles?.length) return data.detalles.join(' · ')
  return data?.message || data?.error || error?.message || 'Error inesperado'
}

export default api
