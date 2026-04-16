import { Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useGetCurrentTeamQuery } from '../features/team/teamApi.js'

export function RequireCaptain({ children }) {
  const user = useSelector((state) => state.auth.user)
  const isOrganizer = user?.role === 'ORGANIZER'
  const { data: currentTeam, isFetching, error } = useGetCurrentTeamQuery(undefined, {
    skip: !user || isOrganizer,
  })

  if (!user) {
    return (
      <section className="page-card">
        <p className="page-note">Проверяем доступ...</p>
      </section>
    )
  }

  if (isOrganizer) {
    return <Navigate to="/organizer/games" replace />
  }

  if (isFetching) {
    return (
      <section className="page-card">
        <p className="page-note">Загружаем данные команды...</p>
      </section>
    )
  }

  if (error?.status === 404 || !currentTeam) {
    return <Navigate to="/teams/join" replace />
  }

  const isCaptainById = user?.id && currentTeam.captainId === user.id
  const isCaptainByEmail = user?.email && currentTeam.captainEmail === user.email

  if (!isCaptainById && !isCaptainByEmail) {
    return <Navigate to="/team" replace />
  }

  return children
}
