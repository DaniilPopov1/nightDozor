import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  useCreateJoinRequestMutation,
  useGetOutgoingJoinRequestsQuery,
  useGetTeamsQuery,
  useJoinTeamByCodeMutation,
} from '../features/team/teamApi.js'
import { formatMembershipStatus } from '../shared/lib/formatters.js'

export function JoinTeamPage() {
  const navigate = useNavigate()
  const [inviteCode, setInviteCode] = useState('')
  const [city, setCity] = useState('')
  const [submittedCity, setSubmittedCity] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [requestingTeamId, setRequestingTeamId] = useState(null)
  const { data: teams = [], isFetching: isTeamsLoading, error: teamsError } = useGetTeamsQuery(submittedCity)
  const {
    data: outgoingRequests = [],
    error: outgoingError,
  } = useGetOutgoingJoinRequestsQuery()
  const [joinTeamByCode, { isLoading: isSubmittingCode }] = useJoinTeamByCodeMutation()
  const [createJoinRequest, { isLoading: isSendingRequest }] = useCreateJoinRequestMutation()

  useEffect(() => {
    if (teamsError?.message) {
      setError(teamsError.message)
      return
    }

    if (outgoingError?.message) {
      setError(outgoingError.message)
      return
    }

    setError('')
  }, [outgoingError, teamsError])

  const handleFilterSubmit = async (event) => {
    event.preventDefault()
    setSubmittedCity(city.trim())
  }

  const handleJoinByCode = async (event) => {
    event.preventDefault()

    if (!inviteCode.trim()) {
      setError('Введи код приглашения')
      return
    }

    setError('')
    setMessage('')

    try {
      await joinTeamByCode(inviteCode.trim()).unwrap()
      navigate('/team', { replace: true })
    } catch (requestError) {
      setError(requestError?.message || 'Не удалось вступить в команду по коду')
    }
  }

  const handleSendRequest = async (teamId) => {
    setRequestingTeamId(teamId)
    setMessage('')
    setError('')

    try {
      await createJoinRequest(teamId).unwrap()
      setMessage('Заявка на вступление отправлена')
    } catch (requestError) {
      setError(requestError?.message || 'Не удалось отправить заявку')
    } finally {
      setRequestingTeamId(null)
    }
  }

  const requestedTeamIds = new Set(outgoingRequests.map((request) => request.teamId))

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Команда</p>
        <h1>Найти команду</h1>
        <p className="page-card__text">
          Можно вступить по коду приглашения или отправить запрос в подходящую команду из списка.
        </p>
      </div>

      {error ? <p className="form-message form-message--error">{error}</p> : null}
      {message ? <p className="form-message form-message--success">{message}</p> : null}

      <div className="split-grid">
        <section className="section-block">
          <h2>Вступить по коду</h2>

          <form className="auth-form" onSubmit={handleJoinByCode}>
            <label className="field">
              <span>Код приглашения</span>
              <input
                type="text"
                placeholder="Введите invite-код"
                value={inviteCode}
                onChange={(event) => setInviteCode(event.target.value)}
              />
            </label>

            <button className="button button--primary" type="submit" disabled={isSubmittingCode}>
              {isSubmittingCode ? 'Подключаем...' : 'Вступить'}
            </button>
          </form>
        </section>

        <section className="section-block">
          <h2>Поиск команд</h2>

          <form className="inline-form" onSubmit={handleFilterSubmit}>
            <label className="field field--inline">
              <span>Город</span>
              <input
                type="text"
                placeholder="Например, Казань"
                value={city}
                onChange={(event) => setCity(event.target.value)}
              />
            </label>

            <button className="button button--secondary" type="submit">
              Найти
            </button>
          </form>

          {isTeamsLoading ? <p className="page-note">Загрузка списка команд...</p> : null}
          {submittedCity ? <p className="page-note">Город поиска: {submittedCity}</p> : null}
          {!isTeamsLoading && teams.length === 0 ? (
            <p className="page-note">
              {submittedCity
                ? 'В этом городе пока нет доступных команд.'
                : 'Команды пока не найдены. Попробуй указать город для поиска.'}
            </p>
          ) : null}

          <div className="list-grid">
            {teams.map((team) => (
              <article key={team.id} className="list-card">
                <h3>{team.name}</h3>
                <p>Город: {team.city}</p>
                <p>Капитан: {team.captainEmail}</p>
                <p>Участников: {team.activeMembersCount}</p>

                <button
                  className="button button--secondary"
                  type="button"
                  onClick={() => handleSendRequest(team.id)}
                  disabled={
                    requestingTeamId === team.id ||
                    requestedTeamIds.has(team.id) ||
                    isSendingRequest
                  }
                >
                  {requestedTeamIds.has(team.id)
                    ? 'Заявка уже отправлена'
                    : requestingTeamId === team.id
                      ? 'Отправляем...'
                      : 'Отправить заявку'}
                </button>
              </article>
            ))}
          </div>
        </section>
      </div>

      {outgoingRequests.length > 0 ? (
        <section className="section-block">
          <h2>Мои отправленные заявки</h2>
          <div className="list-grid">
            {outgoingRequests.map((request) => (
              <article key={request.teamId} className="list-card">
                <h3>{request.teamName}</h3>
                <p>Город: {request.city}</p>
                <p>Статус: {formatMembershipStatus(request.status)}</p>
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </section>
  )
}
