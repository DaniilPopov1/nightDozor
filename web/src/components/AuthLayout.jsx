import { Outlet, useLocation } from 'react-router-dom'

export function AuthLayout() {
  const location = useLocation()
  const showIntro = location.pathname === '/login' || location.pathname === '/register'

  return (
    <div className="shell">
      <div className="shell__glow shell__glow--top" aria-hidden="true" />
      <div className="shell__glow shell__glow--bottom" aria-hidden="true" />

      <main className="shell__content">
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
