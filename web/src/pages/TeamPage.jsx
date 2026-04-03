import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
import {
  useGetCurrentTeamQuery,
  useGetIncomingJoinRequestsQuery,
  useGetOutgoingJoinRequestsQuery,
  useLeaveTeamMutation,
} from '../features/team/teamApi.js'

function formatDate(value) {
  if (!value) {
    return 'Не указано'
  }

  return new Date(value).toLocaleString('ru-RU')
}

export function TeamPage() {
  const currentUser = useSelector((state) => state.auth.user)
  const [actionMessage, setActionMessage] = useState('')
  const {
    data: team,
    isLoading: isTeamLoading,
    error: teamError,
  } = useGetCurrentTeamQuery()
  const { data: incomingRequests = [] } = useGetIncomingJoinRequestsQuery(undefined, {
    skip: !team,
  })
  const { data: outgoingRequests = [] } = useGetOutgoingJoinRequestsQuery()
  const [leaveTeam, { isLoading: isLeaving }] = useLeaveTeamMutation()

  const isCaptain = useMemo(() => {
    if (!team || !currentUser?.id) {
      return false
    }

    return team.captainId === currentUser.id
  }, [currentUser?.id, team])

  const handleLeaveTeam = async () => {
    setActionMessage('')

    try {
      const response = await leaveTeam().unwrap()
      setActionMessage(response.message || 'Вы вышли из команды')
    } catch (requestError) {
      setActionMessage(requestError?.message || 'Не удалось выйти из команды')
    }
  }

  const error = teamError?.status && teamError.status !== 404 ? teamError.message : ''
  const status = isTeamLoading ? 'loading' : 'succeeded'

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Команда</p>
        <h1>Управление командой</h1>
        <p className="page-card__text">
          Здесь можно посмотреть состав команды, код приглашения и текущие заявки
          на вступление.
        </p>
      </div>

      {status === 'loading' ? <p className="page-note">Загрузка команды...</p> : null}
      {error ? <p className="form-message form-message--error">{error}</p> : null}
      {actionMessage ? <p className="form-message form-message--success">{actionMessage}</p> : null}

      {!team && status === 'succeeded' ? (
        <>
          <section className="empty-state">
            <h2>У тебя пока нет команды</h2>
            <p>
              Создай новую команду или присоединись к существующей по коду
              приглашения либо через список команд.
            </p>

            <div className="cta-group">
              <Link className="button button--primary" to="/teams/create">
                Создать команду
              </Link>
              <Link className="button button--secondary" to="/teams/join">
                Найти команду
              </Link>
            </div>
          </section>

          {outgoingRequests.length > 0 ? (
            <section className="section-block">
              <h2>Мои заявки на вступление</h2>
              <div className="list-grid">
                {outgoingRequests.map((request) => (
                  <article key={request.teamId} className="list-card">
                    <h3>{request.teamName}</h3>
                    <p>{request.city}</p>
                    <p>Статус: {request.status}</p>
                    <p>Отправлена: {formatDate(request.createdAt)}</p>
                  </article>
                ))}
              </div>
            </section>
          ) : null}
        </>
      ) : null}

      {team ? (
        <div className="stack">
          <section className="section-block">
            <div className="section-block__header">
              <div>
                <h2>{team.name}</h2>
                <p className="section-block__text">Город: {team.city}</p>
              </div>
              {!isCaptain ? (
                <button
                  className="button button--secondary"
                  type="button"
                  onClick={handleLeaveTeam}
                  disabled={isLeaving}
                >
                  {isLeaving ? 'Выходим...' : 'Покинуть команду'}
                </button>
              ) : null}
            </div>

            <dl className="detail-grid">
              <div>
                <dt>Капитан</dt>
                <dd>{team.captainEmail}</dd>
              </div>
              <div>
                <dt>Код приглашения</dt>
                <dd>{team.inviteCode}</dd>
              </div>
              <div>
                <dt>Дата создания</dt>
                <dd>{formatDate(team.createdAt)}</dd>
              </div>
              <div>
                <dt>Роль в команде</dt>
                <dd>{isCaptain ? 'Капитан' : 'Участник'}</dd>
              </div>
            </dl>
          </section>

          <section className="section-block">
            <h2>Участники команды</h2>
            <div className="list-grid">
              {team.members.map((member) => (
                <article key={member.userId} className="list-card">
                  <h3>{member.email}</h3>
                  <p>Роль: {member.role}</p>
                  <p>Статус: {member.status}</p>
                  <p>Вступил: {formatDate(member.joinedAt)}</p>
                </article>
              ))}
            </div>
          </section>

          {isCaptain && incomingRequests.length > 0 ? (
            <section className="section-block">
              <h2>Входящие заявки</h2>
              <div className="list-grid">
                {incomingRequests.map((request) => (
                  <article key={request.userId} className="list-card">
                    <h3>{request.userEmail}</h3>
                    <p>Статус: {request.status}</p>
                    <p>Отправлена: {formatDate(request.createdAt)}</p>
                    <p className="section-block__hint">
                      Подтверждение и отклонение добавим на следующем шаге.
                    </p>
                  </article>
                ))}
              </div>
            </section>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}
