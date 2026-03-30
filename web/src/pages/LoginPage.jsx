import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { clearAuthFeedback, loginUser } from '../features/auth/authSlice.js'

export function LoginPage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { token, loginError, loginStatus } = useSelector((state) => state.auth)
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  })
  const [validationError, setValidationError] = useState('')

  useEffect(() => {
    dispatch(clearAuthFeedback())
  }, [dispatch])

  useEffect(() => {
    if (token) {
      navigate('/', { replace: true })
    }
  }, [navigate, token])

  const handleChange = (event) => {
    const { name, value } = event.target

    setFormData((current) => ({
      ...current,
      [name]: value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!formData.email.trim() || !formData.password.trim()) {
      setValidationError('Заполни email и пароль')
      return
    }

    setValidationError('')
    const resultAction = await dispatch(loginUser(formData))

    if (loginUser.fulfilled.match(resultAction)) {
      navigate('/', { replace: true })
    }
  }

  const isSubmitting = loginStatus === 'loading'

  return (
    <section className="auth-card">
      <p className="auth-card__eyebrow">Авторизация</p>
      <h2>Вход в систему</h2>
      <p className="auth-card__text">
        Войди под своим аккаунтом участника или организатора, чтобы продолжить
        работу с сервисом.
      </p>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Email</span>
          <input
            autoComplete="email"
            name="email"
            type="email"
            placeholder="you@example.com"
            value={formData.email}
            onChange={handleChange}
          />
        </label>

        <label className="field">
          <span>Пароль</span>
          <input
            autoComplete="current-password"
            name="password"
            type="password"
            placeholder="Введите пароль"
            value={formData.password}
            onChange={handleChange}
          />
        </label>

        {validationError ? (
          <p className="form-message form-message--error">{validationError}</p>
        ) : null}
        {loginError ? (
          <p className="form-message form-message--error">{loginError}</p>
        ) : null}

        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Входим...' : 'Войти'}
        </button>
      </form>

      <p className="auth-card__footer">
        Нет аккаунта? <Link to="/register">Зарегистрироваться</Link>
      </p>
    </section>
  )
}
