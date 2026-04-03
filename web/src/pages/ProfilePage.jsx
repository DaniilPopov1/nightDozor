import { useSelector } from 'react-redux'
import { useGetCurrentUserQuery } from '../features/auth/authApi.js'

function formatDate(value) {
  if (!value) {
    return 'Не указано'
  }

  return new Date(value).toLocaleString('ru-RU')
}

export function ProfilePage() {
  const fallbackUser = useSelector((state) => state.auth.user)
  const {
    data: currentUser,
    error,
    isLoading,
  } = useGetCurrentUserQuery()
  const user = currentUser || fallbackUser
  const profileError = error?.message

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Профиль</p>
        <h1>Данные аккаунта</h1>
        <p className="page-card__text">
          На этой странице собраны основные данные пользователя и роль в системе.
        </p>
      </div>

      {profileError ? (
        <p className="form-message form-message--error">{profileError}</p>
      ) : null}

      <dl className="detail-grid">
        <div>
          <dt>Email</dt>
          <dd>{user?.email || (isLoading ? 'Загрузка...' : 'Не указано')}</dd>
        </div>
        <div>
          <dt>Роль в системе</dt>
          <dd>{user?.role || (isLoading ? 'Загрузка...' : 'Не указано')}</dd>
        </div>
        <div>
          <dt>Подтверждение аккаунта</dt>
          <dd>
            {user?.enabled === undefined
              ? 'Загрузка...'
              : user.enabled
                ? 'Подтверждён'
                : 'Ожидает подтверждения'}
          </dd>
        </div>
        <div>
          <dt>Дата регистрации</dt>
          <dd>{formatDate(user?.createdAt)}</dd>
        </div>
      </dl>
    </section>
  )
}
