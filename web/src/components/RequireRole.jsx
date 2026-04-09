import { Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'

export function RequireRole({ role, children }) {
  const user = useSelector((state) => state.auth.user)

  if (!user) {
    return null
  }

  if (user.role !== role) {
    return <Navigate to="/profile" replace />
  }

  return children
}
