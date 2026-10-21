import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Chatbot from './Chatbot.jsx'

const NAV = {
  ESTUDIANTE: [
    { to: '/tickets', label: 'Mis tickets' },
    { to: '/tickets/nuevo', label: 'Nuevo ticket' },
  ],
  DOCENTE: [
    { to: '/tickets', label: 'Mis tickets' },
    { to: '/tickets/nuevo', label: 'Nuevo ticket' },
  ],
  TECNICO: [{ to: '/tickets', label: 'Tickets asignados' }],
  ADMINISTRADOR: [
    { to: '/tickets', label: 'Todos los tickets' },
    { to: '/usuarios', label: 'Usuarios' },
    { to: '/categorias', label: 'Categorías' },
    { to: '/reportes', label: 'Reportes' },
  ],
}

const ETIQUETA_ROL = {
  ESTUDIANTE: 'Estudiante',
  DOCENTE: 'Docente',
  TECNICO: 'Técnico',
  ADMINISTRADOR: 'Administrador',
}

export default function Layout() {
  const { usuario, rol, logout } = useAuth()
  const navigate = useNavigate()

  function salir() {
    logout()
    navigate('/login', { replace: true })
  }

  const items = NAV[rol] ?? []

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">ASM</span>
          <div>
            <strong>HelpDesk</strong>
            <small>Cibertec</small>
          </div>
        </div>

        <nav className="nav">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/tickets'}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-box">
            <span className="avatar">{usuario?.correo?.[0]?.toUpperCase()}</span>
            <div>
              <strong>{usuario?.correo}</strong>
              <small>{ETIQUETA_ROL[rol] ?? rol}</small>
            </div>
          </div>
          <button className="btn btn-ghost" onClick={salir}>
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="main">
        <Outlet />
      </main>

      {/* Asistente IA flotante (IBM watsonx) */}
      <Chatbot />
    </div>
  )
}
