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
    return null
  }

  if (isOrganizer) {
    return <Navigate to="/organizer/games" replace />
  }

  if (isFetching) {
    return null
  }

  if (error?.status === 404 || !currentTeam) {
    return <Navigate to="/teams/join" replace />
  }

  if (currentTeam.captainId !== user.id) {
    return <Navigate to="/team" replace />
  }

  return children
}
