import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

/** Protege una ruta y, opcionalmente, restringe el acceso a ciertos roles. */
export default function ProtectedRoute({ roles, children }) {
  const { autenticado, rol } = useAuth()
  const location = useLocation()

  if (!autenticado) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (roles && !roles.includes(rol)) {
    return <Navigate to="/" replace />
  }

  return children
}
