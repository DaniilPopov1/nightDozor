import { Navigate, Route, Routes } from 'react-router-dom'
import { useSelector } from 'react-redux'
import './App.css'
import { AuthLayout } from './components/AuthLayout.jsx'
import { RequireAuth } from './components/RequireAuth.jsx'
import { CreateTeamPage } from './pages/CreateTeamPage.jsx'
import { GameDetailsPage } from './pages/GameDetailsPage.jsx'
import { GamesPage } from './pages/GamesPage.jsx'
import { JoinTeamPage } from './pages/JoinTeamPage.jsx'
import { LoginPage } from './pages/LoginPage.jsx'
import { ProfilePage } from './pages/ProfilePage.jsx'
import { RegisterPage } from './pages/RegisterPage.jsx'
import { TeamPage } from './pages/TeamPage.jsx'

function RootRedirect() {
  const token = useSelector((state) => state.auth.token)

  return <Navigate to={token ? '/profile' : '/login'} replace />
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
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
