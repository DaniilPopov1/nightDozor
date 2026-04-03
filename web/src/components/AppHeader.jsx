import { NavLink } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { logout } from '../features/auth/authSlice.js'
import { useGetCurrentTeamQuery } from '../features/team/teamApi.js'
import { apiSlice } from '../shared/api/apiSlice.js'

export function AppHeader() {
  const dispatch = useDispatch()
  const user = useSelector((state) => state.auth.user)
  const { data: currentTeam } = useGetCurrentTeamQuery()
  const hasTeam = Boolean(currentTeam)

  const navItems = [
    { to: '/profile', label: 'Профиль' },
    hasTeam
      ? { to: '/team', label: 'Моя команда' }
      : { to: '/teams/join', label: 'Найти команду' },
    { to: '/games', label: 'Игры' },
  ]

  return (
    <header className="app-header">
      <div className="app-header__brand">
        <span className="app-header__title">Night Dozor</span>
        <span className="app-header__subtitle">
          {user?.email || 'Авторизованный пользователь'}
        </span>
      </div>

      <nav className="app-header__nav" aria-label="Основная навигация">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              isActive ? 'app-header__link app-header__link--active' : 'app-header__link'
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <button
        className="button button--secondary app-header__logout"
        type="button"
        onClick={() => {
          dispatch(logout())
          dispatch(apiSlice.util.resetApiState())
        }}
      >
        Выйти
      </button>
    </header>
  )
}
