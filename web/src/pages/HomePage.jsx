import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { loadCurrentUser, logout } from '../features/auth/authSlice.js'

export function HomePage() {
  const dispatch = useDispatch()
  const { token, user, profileStatus, profileError } = useSelector(
    (state) => state.auth,
  )

  useEffect(() => {
    if (token && !user && profileStatus === 'idle') {
      dispatch(loadCurrentUser())
    }
  }, [dispatch, profileStatus, token, user])

  const handleLogout = () => {
    dispatch(logout())
  }

  if (!token) {
    return (
      <>
        <section className="intro-card">
          <p className="intro-card__eyebrow">Night Dozor</p>
          <h1>Сервис для управления городской игрой</h1>
          <p className="intro-card__text">
            Платформа помогает организаторам запускать игры, а участникам
            быстро собирать команды, подавать заявки и проходить задания в
            одном удобном интерфейсе.
          </p>
        </section>

        <section className="auth-card auth-card--wide">
          <p className="auth-card__eyebrow">Старт</p>
          <h2>Выбери удобный способ входа в сервис</h2>
          <p className="auth-card__text">
            Зарегистрируй новый аккаунт или войди в уже существующий, чтобы
            продолжить работу с командами и играми.
          </p>

          <div className="cta-group">
            <Link className="button button--primary" to="/login">
              Войти
            </Link>
            <Link className="button button--secondary" to="/register">
              Зарегистрироваться
            </Link>
          </div>
        </section>
      </>
    )
  }

  return (
    <div className="dashboard-preview">
      <section className="coming-soon-card">
        <p className="coming-soon-card__eyebrow">Night Dozor</p>
        <h2>Веб-приложение скоро будет готово</h2>
        <p className="coming-soon-card__text">
          Сейчас мы подготавливаем основной интерфейс сервиса для организаторов
          и участников игры. Скоро здесь появятся полноценные рабочие разделы.
        </p>
      </section>

      <section className="auth-card auth-card--wide">
        <p className="auth-card__eyebrow">Профиль</p>
        <h2>Вы уже авторизованы</h2>
        <p className="auth-card__text">
          Данные текущего пользователя загружены.
        </p>

        <dl className="profile-list">
          <div>
            <dt>Email</dt>
            <dd>{user?.email || 'Загрузка...'}</dd>
          </div>
          <div>
            <dt>Роль</dt>
            <dd>{user?.role || 'Загрузка...'}</dd>
          </div>
        </dl>

        {profileError ? (
          <p className="form-message form-message--error">{profileError}</p>
        ) : null}

        <div className="cta-group">
          <button
            className="button button--secondary"
            type="button"
            onClick={handleLogout}
          >
            Выйти
          </button>
        </div>
      </section>
    </div>
  )
}
