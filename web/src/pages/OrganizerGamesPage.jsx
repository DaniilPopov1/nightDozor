import { Link } from 'react-router-dom'
import { useGetOrganizerGamesQuery } from '../features/game/gameApi.js'
import { formatDateTime, formatGameStatus } from '../shared/lib/formatters.js'

export function OrganizerGamesPage() {
  const { data: games = [], isFetching, error } = useGetOrganizerGamesQuery()

  return (
    <section className="page-card">
      <div className="page-card__header page-card__header--row">
        <div>
          <p className="page-card__eyebrow">Организатор</p>
          <h1>Мои игры</h1>
          <p className="page-card__text">
            Здесь собраны все игры, которые ты создал как организатор. Из этого раздела удобно
            переходить к настройке маршрутов, заданий и заявок команд.
          </p>
        </div>

        <Link className="button button--primary" to="/organizer/games/create">
          Создать игру
        </Link>
      </div>

      {error?.message ? <p className="form-message form-message--error">{error.message}</p> : null}
      {isFetching ? <p className="page-note">Загрузка игр организатора...</p> : null}

      {games.length === 0 && !isFetching ? (
        <section className="empty-state">
          <h2>Игр пока нет</h2>
          <p>Создай первую игру, чтобы задать даты, количество маршрутов и начать приём заявок от команд.</p>
          <div className="cta-group">
            <Link className="button button--primary" to="/organizer/games/create">
              Создать первую игру
            </Link>
          </div>
        </section>
      ) : null}

      <div className="list-grid">
        {games.map((game) => (
          <article key={game.id} className="list-card">
            <h2>{game.title}</h2>
            <p>Город: {game.city}</p>
            <p>Статус: {formatGameStatus(game.status)}</p>
            <p>
              Размер команды: {game.minTeamSize}-{game.maxTeamSize}
            </p>
            <p>Маршрутов: {game.routeSlotsCount}</p>
            <p>Регистрация до: {formatDateTime(game.registrationEndsAt)}</p>
            <p>Старт: {formatDateTime(game.startsAt)}</p>
            <div className="cta-group">
              {game.status === 'CANCELED' ? (
                <span className="button button--secondary button--disabled" aria-disabled="true">
                  Игра отменена
                </span>
              ) : (
                <Link className="button button--secondary" to={`/organizer/games/${game.id}`}>
                  {game.status === 'FINISHED' ? 'Открыть результаты' : 'Открыть игру'}
                </Link>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
