import { useOutletContext } from 'react-router-dom'
import {
  useGetOrganizerGameResultsQuery,
  useGetOrganizerGameStandingsQuery,
} from '../features/game/gameApi.js'
import { formatDateTime } from '../shared/lib/formatters.js'

const POLLING_INTERVAL_MS = 10_000

function formatDuration(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  return [hours, minutes, seconds]
    .map((value) => String(value).padStart(2, '0'))
    .join(':')
}

function StandingsTable({ standings, isLive }) {
  if (standings.length === 0) {
    return (
      <section className="empty-state">
        <h2>{isLive ? 'Пока нет данных' : 'Результатов пока нет'}</h2>
        <p>
          {isLive
            ? 'Команды ещё не начали прохождение маршрутов.'
            : 'Для этой игры ещё не сформирован итоговый зачёт.'}
        </p>
      </section>
    )
  }

  return (
    <div className="list-grid">
      {standings.map((team) => (
        <article key={team.teamId} className="list-card">
          <h3>
            #{team.place} {team.teamName}
          </h3>
          <p>
            Пройдено этапов: {team.completedTasksCount} из {team.totalTasksCount}
          </p>
          <p>Штраф: {team.totalPenaltyMinutes} мин.</p>
          <p>Время прохождения: {formatDuration(team.elapsedSeconds)}</p>
          <p>Итоговый score: {formatDuration(team.totalScoreSeconds)}</p>
          <p>Статус сессии: {team.sessionStatus}</p>
          <p>
            Завершение:{' '}
            {team.finishedAt ? formatDateTime(team.finishedAt) : 'Команда не завершила игру'}
          </p>
        </article>
      ))}
    </div>
  )
}

function LiveStandings({ gameId }) {
  const {
    data: standings = [],
    isFetching,
    error,
  } = useGetOrganizerGameStandingsQuery(gameId, {
    pollingInterval: POLLING_INTERVAL_MS,
  })

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Live-рейтинг</h2>
          <p className="section-block__text">
            Таблица обновляется автоматически каждые {POLLING_INTERVAL_MS / 1000} секунд.
          </p>
        </div>
      </div>

      {error?.message ? (
        <p className="form-message form-message--error">{error.message}</p>
      ) : null}
      {isFetching && standings.length === 0 ? (
        <p className="page-note">Загрузка рейтинга...</p>
      ) : null}

      <StandingsTable standings={standings} isLive />
    </section>
  )
}

function FinishedResults({ gameId }) {
  const {
    data: standings = [],
    isFetching,
    error,
  } = useGetOrganizerGameResultsQuery(gameId)

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Результаты игры</h2>
          <p className="section-block__text">
            Здесь собран итоговый зачёт команд по завершённой игре.
          </p>
        </div>
      </div>

      {error?.message ? (
        <p className="form-message form-message--error">{error.message}</p>
      ) : null}
      {isFetching ? <p className="page-note">Загрузка результатов игры...</p> : null}

      {!isFetching ? <StandingsTable standings={standings} isLive={false} /> : null}
    </section>
  )
}

export function OrganizerGameResultsPage() {
  const { game } = useOutletContext()

  if (game.status === 'IN_PROGRESS') {
    return <LiveStandings gameId={game.id} />
  }

  return <FinishedResults gameId={game.id} />
}
