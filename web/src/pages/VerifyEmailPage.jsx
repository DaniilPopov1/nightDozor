import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { useVerifyEmailMutation } from '../features/auth/authApi.js'

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [verifyEmail] = useVerifyEmailMutation()
  const [status, setStatus] = useState('loading') // loading | success | error
  const [message, setMessage] = useState('')

  useEffect(() => {
    if (!token) {
      setStatus('error')
      setMessage('Токен подтверждения не найден.')
      return
    }

    verifyEmail(token)
      .unwrap()
      .then(() => {
        setStatus('success')
      })
      .catch((error) => {
        setStatus('error')
        setMessage(error?.message || 'Не удалось подтвердить почту. Возможно, ссылка устарела.')
      })
  }, [token, verifyEmail])

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Night Dozor</p>
        {status === 'loading' && <h1>Подтверждение почты...</h1>}
        {status === 'success' && <h1>Почта подтверждена</h1>}
        {status === 'error' && <h1>Ошибка подтверждения</h1>}
      </div>

      {status === 'loading' && (
        <p className="page-card__text">Пожалуйста, подождите...</p>
      )}

      {status === 'success' && (
        <>
          <p className="page-card__text">
            Твой аккаунт успешно подтверждён. Теперь можно войти в систему.
          </p>
          <Link className="button button--primary" to="/login">
            Войти
          </Link>
        </>
      )}

      {status === 'error' && (
        <>
          <p className="form-message form-message--error">{message}</p>
          <Link className="button button--secondary" to="/login">
            Вернуться на страницу входа
          </Link>
        </>
      )}
    </section>
  )
}
