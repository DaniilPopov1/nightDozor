import { Navigate, Route, Routes } from 'react-router-dom'
import { useSelector } from 'react-redux'
import './App.css'
import { AuthLayout } from './components/AuthLayout.jsx'
import { RequireAuth } from './components/RequireAuth.jsx'
import { RequireRole } from './components/RequireRole.jsx'
import { CreateTeamPage } from './pages/CreateTeamPage.jsx'
import { GameDetailsPage } from './pages/GameDetailsPage.jsx'
import { GamesPage } from './pages/GamesPage.jsx'
import { JoinTeamPage } from './pages/JoinTeamPage.jsx'
import { LoginPage } from './pages/LoginPage.jsx'
import { OrganizerCreateGamePage } from './pages/OrganizerCreateGamePage.jsx'
import { OrganizerGameDetailsPage } from './pages/OrganizerGameDetailsPage.jsx'
import { OrganizerGamesPage } from './pages/OrganizerGamesPage.jsx'
import { ProfilePage } from './pages/ProfilePage.jsx'
import { RegisterPage } from './pages/RegisterPage.jsx'
import { TeamPage } from './pages/TeamPage.jsx'

function RootRedirect() {
  const token = useSelector((state) => state.auth.token)
  const user = useSelector((state) => state.auth.user)

  if (!token) {
    return <Navigate to="/login" replace />
  }

  if (user?.role === 'ORGANIZER') {
    return <Navigate to="/organizer/games" replace />
  }

  return <Navigate to="/profile" replace />
}

function App() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route
          path="/profile"
          element={
            <RequireAuth>
              <ProfilePage />
            </RequireAuth>
          }
        />
        <Route
          path="/team"
          element={
            <RequireAuth>
              <TeamPage />
            </RequireAuth>
          }
        />
        <Route
          path="/teams/create"
          element={
            <RequireAuth>
              <CreateTeamPage />
            </RequireAuth>
          }
        />
        <Route
          path="/teams/join"
          element={
            <RequireAuth>
              <JoinTeamPage />
            </RequireAuth>
          }
        />
        <Route
          path="/games"
          element={
            <RequireAuth>
              <GamesPage />
            </RequireAuth>
          }
        />
        <Route
          path="/games/:gameId"
          element={
            <RequireAuth>
              <GameDetailsPage />
            </RequireAuth>
          }
        />
        <Route
          path="/organizer/games"
          element={
            <RequireAuth>
              <RequireRole role="ORGANIZER">
                <OrganizerGamesPage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/organizer/games/create"
          element={
            <RequireAuth>
              <RequireRole role="ORGANIZER">
                <OrganizerCreateGamePage />
              </RequireRole>
            </RequireAuth>
          }
        />
        <Route
          path="/organizer/games/:gameId"
          element={
            <RequireAuth>
              <RequireRole role="ORGANIZER">
                <OrganizerGameDetailsPage />
              </RequireRole>
            </RequireAuth>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
