import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useGetGamesQuery, useGetMyTeamRegistrationsQuery } from '../features/game/gameApi.js'
import { useGetCurrentTeamQuery } from '../features/team/teamApi.js'

function formatDate(value) {
  if (!value) {
    return 'Не указано'
  }

  return new Date(value).toLocaleString('ru-RU')
}

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
              <p>Статус игры: {game.status}</p>
              <p>
                Размер команды: {game.minTeamSize}-{game.maxTeamSize}
              </p>
              <p>Старт: {formatDate(game.startsAt)}</p>
              <p>
                Заявка: {registration ? registration.registrationStatus : 'Не подана'}
              </p>

              <div className="cta-group">
                <Link className="button button--secondary" to={`/games/${game.id}`}>
                  Открыть игру
                </Link>
                {!team ? (
                  <span className="badge">Сначала нужна команда</span>
                ) : null}
                {team && !isCaptain ? <span className="badge">Заявку подаёт капитан</span> : null}
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
