import { useOutletContext } from 'react-router-dom'
import { useGetOrganizerGameResultsQuery } from '../features/game/gameApi.js'
import { formatDateTime } from '../shared/lib/formatters.js'

function formatDuration(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  return [hours, minutes, seconds]
    .map((value) => String(value).padStart(2, '0'))
    .join(':')
}

export function OrganizerGameResultsPage() {
  const { game } = useOutletContext()
  const {
    data: standings = [],
    isFetching,
    error,
  } = useGetOrganizerGameResultsQuery(game.id, {
    skip: game.status !== 'FINISHED',
  })

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

      {error?.message ? <p className="form-message form-message--error">{error.message}</p> : null}
      {isFetching ? <p className="page-note">Загрузка результатов игры...</p> : null}

      {!isFetching && standings.length === 0 ? (
        <section className="empty-state">
          <h2>Результатов пока нет</h2>
          <p>Для этой игры ещё не сформирован итоговый зачёт.</p>
        </section>
      ) : null}

      {standings.length > 0 ? (
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
      ) : null}
    </section>
  )
}
