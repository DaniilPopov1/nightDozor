import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  clearAuthFeedback,
  registerUser,
} from '../features/auth/authSlice.js'

const initialFormState = {
  email: '',
  password: '',
  confirmPassword: '',
  role: 'PARTICIPANT',
}

export function RegisterPage() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { registerError, registerMessage, registerStatus } = useSelector(
    (state) => state.auth,
  )
  const [formData, setFormData] = useState(initialFormState)
  const [validationError, setValidationError] = useState('')

  useEffect(() => {
    dispatch(clearAuthFeedback())
  }, [dispatch])

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

    if (formData.password.length < 8) {
      setValidationError('Пароль должен содержать минимум 8 символов')
      return
    }

    if (formData.password !== formData.confirmPassword) {
      setValidationError('Пароли не совпадают')
      return
    }

    setValidationError('')

    const payload = {
      email: formData.email.trim(),
      password: formData.password,
      role: formData.role,
    }

    const resultAction = await dispatch(registerUser(payload))

    if (registerUser.fulfilled.match(resultAction)) {
      setFormData(initialFormState)
      window.setTimeout(() => navigate('/login'), 1200)
    }
  }

  const isSubmitting = registerStatus === 'loading'

  return (
    <section className="auth-card">
      <p className="auth-card__eyebrow">Регистрация</p>
      <h2>Создание аккаунта</h2>
      <p className="auth-card__text">
        Зарегистрируйся как организатор или участник. После регистрации нужно
        будет подтвердить email.
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
          <span>Роль</span>
          <select name="role" value={formData.role} onChange={handleChange}>
            <option value="PARTICIPANT">Участник</option>
            <option value="ORGANIZER">Организатор</option>
          </select>
        </label>

        <label className="field">
          <span>Пароль</span>
          <input
            autoComplete="new-password"
            name="password"
            type="password"
            placeholder="Минимум 8 символов"
            value={formData.password}
            onChange={handleChange}
          />
        </label>

        <label className="field">
          <span>Повтори пароль</span>
          <input
            autoComplete="new-password"
            name="confirmPassword"
            type="password"
            placeholder="Повтори пароль"
            value={formData.confirmPassword}
            onChange={handleChange}
          />
        </label>

        {validationError ? (
          <p className="form-message form-message--error">{validationError}</p>
        ) : null}
        {registerError ? (
          <p className="form-message form-message--error">{registerError}</p>
        ) : null}
        {registerMessage ? (
          <p className="form-message form-message--success">{registerMessage}</p>
        ) : null}

        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Регистрируем...' : 'Зарегистрироваться'}
        </button>
      </form>

      <p className="auth-card__footer">
        Уже есть аккаунт? <Link to="/login">Войти</Link>
      </p>
    </section>
  )
}
