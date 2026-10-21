import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './routes/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Tickets from './pages/Tickets.jsx'
import NuevoTicket from './pages/NuevoTicket.jsx'
import TicketDetail from './pages/TicketDetail.jsx'
import Usuarios from './pages/Usuarios.jsx'
import Categorias from './pages/Categorias.jsx'
import Reportes from './pages/Reportes.jsx'
import NotFound from './pages/NotFound.jsx'

const ADMIN = ['ADMINISTRADOR']
const SOLICITANTES = ['ESTUDIANTE', 'DOCENTE']

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/tickets" replace />} />
        <Route path="tickets" element={<Tickets />} />
        <Route
          path="tickets/nuevo"
          element={
            <ProtectedRoute roles={SOLICITANTES}>
              <NuevoTicket />
            </ProtectedRoute>
          }
        />
        <Route path="tickets/:id" element={<TicketDetail />} />
        <Route
          path="usuarios"
          element={
            <ProtectedRoute roles={ADMIN}>
              <Usuarios />
            </ProtectedRoute>
          }
        />
        <Route
          path="categorias"
          element={
            <ProtectedRoute roles={ADMIN}>
              <Categorias />
            </ProtectedRoute>
          }
        />
        <Route
          path="reportes"
          element={
            <ProtectedRoute roles={ADMIN}>
              <Reportes />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
