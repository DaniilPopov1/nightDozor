import { useMemo, useState } from 'react'
import { useOutletContext, useParams } from 'react-router-dom'
import {
  useDeleteRouteMutation,
  useGenerateRoutesMutation,
  useGetOrganizerGameRoutesQuery,
  useGetOrganizerGameTasksQuery,
} from '../features/game/gameApi.js'

export function OrganizerGameRoutesPage() {
  const { gameId } = useParams()
  const { game, canManageContent } = useOutletContext()
  const { data: tasks = [], isFetching: isFetchingTasks, error: tasksLoadError } = useGetOrganizerGameTasksQuery(gameId)
  const { data: routes = [], isFetching: isFetchingRoutes, error: routesLoadError } = useGetOrganizerGameRoutesQuery(gameId)
  const [generateRoutes, { isLoading: isGeneratingRoutes }] = useGenerateRoutesMutation()
  const [deleteRoute, { isLoading: isDeletingRoute }] = useDeleteRouteMutation()
  const [message, setMessage] = useState('')
  const [requestError, setRequestError] = useState('')

  const slotNumbers = useMemo(
    () => Array.from({ length: game?.routeSlotsCount || 0 }, (_, index) => index + 1),
    [game?.routeSlotsCount],
  )
  const routesBySlot = useMemo(
    () => new Map(routes.map((route) => [String(route.slotNumber), route])),
    [routes],
  )
  const routeTaskCounts = useMemo(() => routes.map((route) => route.items.length), [routes])
  const areRouteLengthsConsistent = useMemo(() => {
    if (routeTaskCounts.length !== game?.routeSlotsCount || routeTaskCounts.length === 0) {
      return false
    }
    return routeTaskCounts.every((count) => count === routeTaskCounts[0] && count > 0)
  }, [game?.routeSlotsCount, routeTaskCounts])

  const handleGenerateRoutes = async () => {
    setRequestError('')
    setMessage('')
    try {
      await generateRoutes({ gameId }).unwrap()
      setMessage('Маршруты сгенерированы автоматически с циклическим сдвигом')
    } catch (generateError) {
      setRequestError(generateError?.message || 'Не удалось сгенерировать маршруты')
    }
  }

  const handleDeleteRoute = async (routeId) => {
    setRequestError('')
    setMessage('')
    try {
      await deleteRoute({ gameId, routeId }).unwrap()
      setMessage('Маршрут удалён')
    } catch (routeError) {
      setRequestError(routeError?.message || 'Не удалось удалить маршрут')
    }
  }

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Маршруты команд</h2>
          <p className="section-block__text">
            Все команды получат одинаковый набор заданий, но в разном порядке (циклический сдвиг).
            Это гарантирует честность и минимизирует пересечения команд на точках.
            Количество заданий должно быть не меньше количества команд.
          </p>
        </div>
        <button
          className="button button--primary"
          type="button"
          onClick={handleGenerateRoutes}
          disabled={!canManageContent || isGeneratingRoutes || tasks.length === 0}
        >
          {isGeneratingRoutes ? 'Генерируем...' : 'Сгенерировать маршруты'}
        </button>
      </div>

      {tasksLoadError?.message ? <p className="form-message form-message--error">{tasksLoadError.message}</p> : null}
      {routesLoadError?.message ? <p className="form-message form-message--error">{routesLoadError.message}</p> : null}
      {requestError ? <p className="form-message form-message--error">{requestError}</p> : null}
      {message ? <p className="form-message form-message--success">{message}</p> : null}
      {!canManageContent ? (
        <p className="form-message form-message--error">
          После старта или отмены игры маршруты больше нельзя менять.
        </p>
      ) : null}
      {!areRouteLengthsConsistent && routes.length > 0 ? (
        <p className="form-message form-message--error">
          Маршруты не согласованы — нажми «Сгенерировать маршруты» чтобы пересоздать их.
        </p>
      ) : null}

      {isFetchingTasks || isFetchingRoutes ? <p className="page-note">Загрузка маршрутов...</p> : null}

      {slotNumbers.length === 0 ? (
        <section className="empty-state">
          <h2>Количество команд не задано</h2>
          <p>Сначала укажи количество команд в параметрах игры.</p>
        </section>
      ) : null}

      {tasks.length === 0 && slotNumbers.length > 0 && !isFetchingTasks ? (
        <section className="empty-state">
          <h2>Нет заданий</h2>
          <p>Сначала создай задания на вкладке «Задания», затем вернись сюда и нажми «Сгенерировать маршруты».</p>
        </section>
      ) : null}

      <div className="route-editor-list">
        {slotNumbers.map((slotNumber) => {
          const route = routesBySlot.get(String(slotNumber))

          return (
            <article key={slotNumber} className="route-editor">
              <div className="route-editor__header">
                <div className="route-editor__meta">
                  <h3>Маршрут {slotNumber}</h3>
                  <p>{route ? `Заданий: ${route.items.length}` : 'Маршрут ещё не сгенерирован'}</p>
                  {route?.assignedTeamName ? <p>Назначен команде: {route.assignedTeamName}</p> : null}
                </div>

                {route ? (
                  <div className="list-card__actions">
                    <button
                      className="button button--secondary"
                      type="button"
                      onClick={() => handleDeleteRoute(route.id)}
                      disabled={!canManageContent || isDeletingRoute || Boolean(route.assignedTeamId)}
                    >
                      Удалить
                    </button>
                  </div>
                ) : null}
              </div>

              {route ? (
                route.items.length > 0 ? (
                  <div className="route-editor__items">
                    {route.items.map((item) => (
                      <div key={item.id} className="route-editor__item">
                        <strong>{item.orderIndex}. {item.taskTitle}</strong>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="route-editor__empty">
                    <p>В этом маршруте нет заданий.</p>
                  </div>
                )
              ) : (
                <div className="route-editor__empty">
                  <p>Нажми «Сгенерировать маршруты» чтобы создать маршруты для всех команд.</p>
                </div>
              )}
            </article>
          )
        })}
      </div>
    </section>
  )
}
