import { Navigate, Route, Routes } from 'react-router-dom'
import { useSelector } from 'react-redux'
import './App.css'
import { AuthLayout } from './components/AuthLayout.jsx'
import { RequireAuth } from './components/RequireAuth.jsx'
import { RequireCaptain } from './components/RequireCaptain.jsx'
import { RequireRole } from './components/RequireRole.jsx'
import { CreateTeamPage } from './pages/CreateTeamPage.jsx'
import { GameDetailsPage } from './pages/GameDetailsPage.jsx'
import { GamesPage } from './pages/GamesPage.jsx'
import { JoinTeamPage } from './pages/JoinTeamPage.jsx'
import { LoginPage } from './pages/LoginPage.jsx'
import { OrganizerGameLayout } from './components/OrganizerGameLayout.jsx'
import { OrganizerCreateGamePage } from './pages/OrganizerCreateGamePage.jsx'
import { OrganizerGameEditPage } from './pages/OrganizerGameEditPage.jsx'
import { OrganizerGameHintsPage } from './pages/OrganizerGameHintsPage.jsx'
import { OrganizerGameRegistrationsPage } from './pages/OrganizerGameRegistrationsPage.jsx'
import { OrganizerGameRoutesPage } from './pages/OrganizerGameRoutesPage.jsx'
import { OrganizerGameTasksPage } from './pages/OrganizerGameTasksPage.jsx'
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
              <RequireCaptain>
                <GamesPage />
              </RequireCaptain>
            </RequireAuth>
          }
        />
        <Route
          path="/games/:gameId"
          element={
            <RequireAuth>
              <RequireCaptain>
                <GameDetailsPage />
              </RequireCaptain>
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
                <OrganizerGameLayout />
              </RequireRole>
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="edit" replace />} />
          <Route path="edit" element={<OrganizerGameEditPage />} />
          <Route path="tasks" element={<OrganizerGameTasksPage />} />
          <Route path="hints" element={<OrganizerGameHintsPage />} />
          <Route path="routes" element={<OrganizerGameRoutesPage />} />
          <Route path="registrations" element={<OrganizerGameRegistrationsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
