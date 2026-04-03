import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { useLoginMutation } from '../features/auth/authApi.js'
import { setCredentials } from '../features/auth/authSlice.js'

export function LoginPage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { token } = useSelector((state) => state.auth)
  const [login, { isLoading: isSubmitting }] = useLoginMutation()
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  })
  const [validationError, setValidationError] = useState('')
  const [loginError, setLoginError] = useState('')

  useEffect(() => {
    if (token) {
      navigate('/profile', { replace: true })
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
    setLoginError('')

    try {
      const response = await login(formData).unwrap()
      dispatch(setCredentials(response))
      navigate('/profile', { replace: true })
    } catch (requestError) {
      setLoginError(requestError?.message || 'Не удалось выполнить вход')
    }
  }

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
