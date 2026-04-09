import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useGetGamesQuery, useGetMyTeamRegistrationsQuery } from '../features/game/gameApi.js'
import { useGetCurrentTeamQuery } from '../features/team/teamApi.js'
import {
  formatDateTime,
  formatGameStatus,
  formatRegistrationStatus,
} from '../shared/lib/formatters.js'

export function GamesPage() {
  const currentUser = useSelector((state) => state.auth.user)
  const [city, setCity] = useState('')
  const [submittedCity, setSubmittedCity] = useState('')
  const {
    data: games = [],
    isFetching: isGamesLoading,
    error: gamesError,
  } = useGetGamesQuery(submittedCity)
  const { data: registrations = [] } = useGetMyTeamRegistrationsQuery()
  const { data: team } = useGetCurrentTeamQuery()

  const handleSubmit = async (event) => {
    event.preventDefault()
    setSubmittedCity(city.trim())
  }

  const registrationMap = new Map(registrations.map((item) => [item.gameId, item]))
  const isCaptain = Boolean(team && currentUser?.id && team.captainId === currentUser.id)
  const error = gamesError?.message

  const getParticipationHint = (game, registration) => {
    if (registration) {
      const statusMap = {
        PENDING: 'Заявка отправлена и ожидает решения организатора.',
        APPROVED: 'Команда уже подтверждена для участия в этой игре.',
        REJECTED: 'Организатор отклонил заявку команды на эту игру.',
        CANCELED: 'Заявка на участие была отменена.',
      }

      return statusMap[registration.registrationStatus] || 'Статус заявки обновлён.'
    }

    if (!team) {
      return 'Для участия в игре сначала нужно состоять в команде.'
    }

    if (!isCaptain) {
      return 'Подать заявку на участие может только капитан команды.'
    }

    if (team.members.length < game.minTeamSize) {
      return `В команде недостаточно участников: нужно минимум ${game.minTeamSize}.`
    }

    if (team.members.length > game.maxTeamSize) {
      return `В команде слишком много участников: максимум ${game.maxTeamSize}.`
    }

    return 'Команда подходит под условия игры и может подать заявку.'
  }

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Игры</p>
        <h1>Доступные игры</h1>
        <p className="page-card__text">
          Выбирай игру по городу, смотри условия участия и отправляй заявку от имени команды.
        </p>
      </div>

      <form className="inline-form" onSubmit={handleSubmit}>
        <label className="field field--inline">
          <span>Фильтр по городу</span>
          <input
            type="text"
            placeholder="Например, Санкт-Петербург"
            value={city}
            onChange={(event) => setCity(event.target.value)}
          />
        </label>

        <button className="button button--secondary" type="submit">
          Обновить список
        </button>
      </form>

      {error ? <p className="form-message form-message--error">{error}</p> : null}
      {isGamesLoading ? <p className="page-note">Загрузка игр...</p> : null}

      <div className="list-grid">
        {games.map((game) => {
          const registration = registrationMap.get(game.id)

          return (
            <article key={game.id} className="list-card">
              <h2>{game.title}</h2>
              <p>Город: {game.city}</p>
              <p>Статус игры: {formatGameStatus(game.status)}</p>
              <p>
                Размер команды: {game.minTeamSize}-{game.maxTeamSize}
              </p>
              <p>Старт: {formatDateTime(game.startsAt)}</p>
              <p>
                Заявка: {registration ? formatRegistrationStatus(registration.registrationStatus) : 'Не подана'}
              </p>
              <p className="section-block__hint">{getParticipationHint(game, registration)}</p>

              <div className="cta-group">
                <Link className="button button--secondary" to={`/games/${game.id}`}>
                  Открыть игру
                </Link>
                {!team ? <span className="badge">Нужна команда</span> : null}
                {team && !isCaptain ? <span className="badge">Только капитан</span> : null}
                {registration?.registrationStatus === 'APPROVED' ? (
                  <span className="badge badge--success">Команда допущена</span>
                ) : null}
                {registration?.registrationStatus === 'REJECTED' ? (
                  <span className="badge badge--danger">Заявка отклонена</span>
                ) : null}
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
