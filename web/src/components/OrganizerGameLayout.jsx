import { Link, NavLink, Outlet, useParams } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { useGetIncomingRegistrationsQuery, useGetOrganizerGameByIdQuery, useGetOrganizerGameRoutesQuery, useGetOrganizerGameTasksQuery } from '../features/game/gameApi.js'
import { formatDateTime, formatGameStatus } from '../shared/lib/formatters.js'

export function OrganizerGameLayout() {
  const { gameId } = useParams()
  const { data: game, isFetching, error } = useGetOrganizerGameByIdQuery(gameId)
  const { data: tasks = [] } = useGetOrganizerGameTasksQuery(gameId)
  const { data: routes = [] } = useGetOrganizerGameRoutesQuery(gameId)
  const { data: registrations = [] } = useGetIncomingRegistrationsQuery(gameId)
  const [pageOpenedAt] = useState(() => Date.now())

  const hasStartedByTime = game?.startsAt
    ? new Date(game.startsAt).getTime() <= pageOpenedAt
    : false

  const isGameEditable = useMemo(() => {
    if (!game) {
      return false
    }

    if (['IN_PROGRESS', 'FINISHED', 'CANCELED'].includes(game.status)) {
      return false
    }

    return !hasStartedByTime
  }, [game, hasStartedByTime])

  const canManageContent = useMemo(() => {
    if (!game) {
      return false
    }

    return !['IN_PROGRESS', 'FINISHED', 'CANCELED'].includes(game.status)
  }, [game])

  const canReviewRegistrations = canManageContent
  const canCancelGame = Boolean(game && !['FINISHED', 'CANCELED'].includes(game.status))

  return (
    <section className="page-card">
      <div className="page-card__header page-card__header--row">
        <div>
          <p className="page-card__eyebrow">Организатор</p>
          <h1>{game?.title || 'Игра организатора'}</h1>
          <p className="page-card__text">
            Отдельные экраны помогают настраивать игру по шагам: сначала параметры, потом
            задания, подсказки, маршруты и заявки команд.
          </p>
        </div>

        <Link className="button button--secondary" to="/organizer/games">
          Ко всем играм
        </Link>
      </div>

      {error?.message ? <p className="form-message form-message--error">{error.message}</p> : null}
      {isFetching ? <p className="page-note">Загрузка карточки игры...</p> : null}

      {game ? (
        <>
          <section className="section-block">
            <h2>Сводка по игре</h2>
            <dl className="detail-grid detail-grid--spaced">
              <div>
                <dt>Статус</dt>
                <dd>{formatGameStatus(game.status)}</dd>
              </div>
              <div>
                <dt>Маршрутов настроено</dt>
                <dd>
                  {routes.length} из {game.routeSlotsCount}
                </dd>
              </div>
              <div>
                <dt>Заданий создано</dt>
                <dd>{tasks.length}</dd>
              </div>
              <div>
                <dt>Заявок команд</dt>
                <dd>{registrations.length}</dd>
              </div>
              <div>
                <dt>Регистрация до</dt>
                <dd>{formatDateTime(game.registrationEndsAt)}</dd>
              </div>
              <div>
                <dt>Старт игры</dt>
                <dd>{formatDateTime(game.startsAt)}</dd>
              </div>
            </dl>

            <nav className="subnav" aria-label="Навигация по игре">
              <NavLink
                end
                to={`/organizer/games/${gameId}/edit`}
                className={({ isActive }) =>
                  isActive ? 'subnav__link subnav__link--active' : 'subnav__link'
                }
              >
                Игра
              </NavLink>
              <NavLink
                to={`/organizer/games/${gameId}/tasks`}
                className={({ isActive }) =>
                  isActive ? 'subnav__link subnav__link--active' : 'subnav__link'
                }
              >
                Задания
              </NavLink>
              <NavLink
                to={`/organizer/games/${gameId}/hints`}
                className={({ isActive }) =>
                  isActive ? 'subnav__link subnav__link--active' : 'subnav__link'
                }
              >
                Подсказки
              </NavLink>
              <NavLink
                to={`/organizer/games/${gameId}/routes`}
                className={({ isActive }) =>
                  isActive ? 'subnav__link subnav__link--active' : 'subnav__link'
                }
              >
                Маршруты
              </NavLink>
              <NavLink
                to={`/organizer/games/${gameId}/registrations`}
                className={({ isActive }) =>
                  isActive ? 'subnav__link subnav__link--active' : 'subnav__link'
                }
              >
                Заявки команд
              </NavLink>
            </nav>
          </section>

          <Outlet
            context={{
              game,
              isGameEditable,
              canManageContent,
              canReviewRegistrations,
              canCancelGame,
              hasStartedByTime,
            }}
          />
        </>
      ) : null}
    </section>
  )
}
