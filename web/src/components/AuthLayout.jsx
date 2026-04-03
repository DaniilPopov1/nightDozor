import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { logout } from '../features/auth/authSlice.js'
import { useGetCurrentUserQuery } from '../features/auth/authApi.js'
import { apiSlice } from '../shared/api/apiSlice.js'
import { AppHeader } from './AppHeader.jsx'

export function AuthLayout() {
  const dispatch = useDispatch()
  const location = useLocation()
  const token = useSelector((state) => state.auth.token)
  const showIntro = location.pathname === '/login' || location.pathname === '/register'
  const showHeader = Boolean(token) && !showIntro
  const { isError: currentUserError } = useGetCurrentUserQuery(undefined, {
    skip: !token,
  })

  useEffect(() => {
    if (currentUserError && token) {
      dispatch(logout())
      dispatch(apiSlice.util.resetApiState())
    }
  }, [currentUserError, dispatch, token])

  return (
    <div className="shell">
      <div className="shell__glow shell__glow--top" aria-hidden="true" />
      <div className="shell__glow shell__glow--bottom" aria-hidden="true" />

      <main className={showHeader ? 'shell__content shell__content--app' : 'shell__content'}>
        {showHeader ? <AppHeader /> : null}

        {showIntro ? (
          <section className="intro-card">
            <p className="intro-card__eyebrow">Night Dozor</p>
            <h1>Сервис для управления городской игрой</h1>
            <p className="intro-card__text">
              Платформа помогает организаторам запускать игры, а участникам
              быстро собирать команды, подавать заявки и проходить задания в
              одном удобном интерфейсе.
            </p>
          </section>
        ) : null}

        <Outlet />
      </main>
    </div>
  )
}
