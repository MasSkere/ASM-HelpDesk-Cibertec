import { createContext, useContext, useMemo, useState } from 'react'
import api from '../services/api.js'

const AuthContext = createContext(null)

const TOKEN_KEY = 'asm_token'
const USER_KEY = 'asm_user'

function leerUsuarioGuardado() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(leerUsuarioGuardado)

  async function login(correo, password) {
    const { data } = await api.post('/auth/login', { correo, password })
    persistir(data)
    return data
  }

  async function registrar(payload) {
    const { data } = await api.post('/auth/register', payload)
    persistir(data)
    return data
  }

  function persistir(data) {
    const user = { correo: data.correo, rol: data.rol }
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    setUsuario(user)
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setUsuario(null)
  }

  const value = useMemo(
    () => ({
      usuario,
      autenticado: Boolean(usuario),
      rol: usuario?.rol ?? null,
      login,
      registrar,
      logout,
    }),
    [usuario],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
