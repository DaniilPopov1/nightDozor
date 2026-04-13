import { useMemo, useState } from 'react'
import { useOutletContext, useParams } from 'react-router-dom'
import {
  useAddTaskToRouteMutation,
  useCreateRouteMutation,
  useDeleteRouteMutation,
  useGetOrganizerGameRoutesQuery,
  useGetOrganizerGameTasksQuery,
  useRemoveTaskFromRouteMutation,
  useUpdateRouteMutation,
} from '../features/game/gameApi.js'

export function OrganizerGameRoutesPage() {
  const { gameId } = useParams()
  const { game, canManageContent } = useOutletContext()
  const { data: tasks = [], isFetching: isFetchingTasks, error: tasksLoadError } = useGetOrganizerGameTasksQuery(gameId)
  const { data: routes = [], isFetching: isFetchingRoutes, error: routesLoadError } = useGetOrganizerGameRoutesQuery(gameId)
  const [createRoute, { isLoading: isCreatingRoute }] = useCreateRouteMutation()
  const [updateRoute, { isLoading: isUpdatingRoute }] = useUpdateRouteMutation()
  const [deleteRoute, { isLoading: isDeletingRoute }] = useDeleteRouteMutation()
  const [addTaskToRoute, { isLoading: isAddingRouteItem }] = useAddTaskToRouteMutation()
  const [removeTaskFromRoute, { isLoading: isRemovingRouteItem }] = useRemoveTaskFromRouteMutation()
  const [routeForm, setRouteForm] = useState({
    slotNumber: '1',
    name: '',
    taskId: '',
    orderIndex: '1',
  })
  const [editingRouteId, setEditingRouteId] = useState(null)
  const [routeEditName, setRouteEditName] = useState('')
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
  const assignedTaskIds = useMemo(
    () => new Set(routes.flatMap((route) => route.items.map((item) => String(item.taskId)))),
    [routes],
  )
  const availableRouteTasks = useMemo(
    () => tasks.filter((task) => !assignedTaskIds.has(String(task.id))),
    [assignedTaskIds, tasks],
  )
  const routeTaskCounts = useMemo(() => routes.map((route) => route.items.length), [routes])
  const areRouteLengthsConsistent = useMemo(() => {
    if (routeTaskCounts.length !== game?.routeSlotsCount || routeTaskCounts.length === 0) {
      return false
    }

    return routeTaskCounts.every((count) => count === routeTaskCounts[0] && count > 0)
  }, [game?.routeSlotsCount, routeTaskCounts])

  const handleRouteFormChange = (event) => {
    const { name, value } = event.target
    setRouteForm((current) => ({ ...current, [name]: value }))
  }

  const startRouteEditing = (route) => {
    setEditingRouteId(route.id)
    setRouteEditName(route.name)
  }

  const cancelRouteEditing = () => {
    setEditingRouteId(null)
    setRouteEditName('')
  }

  const handleCreateRoute = async (event) => {
    event.preventDefault()
    setRequestError('')
    setMessage('')

    if (!routeForm.slotNumber || !routeForm.name.trim()) {
      setRequestError('Для маршрута выбери номер слота и укажи название')
      return
    }

    try {
      let route = routesBySlot.get(String(routeForm.slotNumber))

      if (!route) {
        route = await createRoute({
          gameId,
          payload: {
            slotNumber: Number(routeForm.slotNumber),
            name: routeForm.name.trim(),
          },
        }).unwrap()
      }

      if (routeForm.taskId) {
        await addTaskToRoute({
          gameId,
          routeId: route.id,
          payload: {
            taskId: Number(routeForm.taskId),
            orderIndex: Number(routeForm.orderIndex),
          },
        }).unwrap()
      }

      setMessage('Маршрут сохранён')
      setRouteForm((current) => ({
        ...current,
        taskId: '',
        orderIndex: String(Number(current.orderIndex) + 1),
      }))
    } catch (routeError) {
      setRequestError(routeError?.message || 'Не удалось сохранить маршрут')
    }
  }

  const handleUpdateRoute = async (routeId) => {
    setRequestError('')
    setMessage('')

    try {
      await updateRoute({
        gameId,
        routeId,
        payload: { name: routeEditName.trim() },
      }).unwrap()
      setMessage('Название маршрута обновлено')
      cancelRouteEditing()
    } catch (routeError) {
      setRequestError(routeError?.message || 'Не удалось обновить маршрут')
    }
  }

  const handleDeleteRoute = async (routeId) => {
    setRequestError('')
    setMessage('')

    try {
      await deleteRoute({ gameId, routeId }).unwrap()
      setMessage('Маршрут удалён')
      if (editingRouteId === routeId) {
        cancelRouteEditing()
      }
    } catch (routeError) {
      setRequestError(routeError?.message || 'Не удалось удалить маршрут')
    }
  }

  const handleRemoveTaskFromRoute = async (routeId, itemId) => {
    setRequestError('')
    setMessage('')

    try {
      await removeTaskFromRoute({ gameId, routeId, itemId }).unwrap()
      setMessage('Задание убрано из маршрута')
    } catch (routeError) {
      setRequestError(routeError?.message || 'Не удалось убрать задание из маршрута')
    }
  }

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Экран настройки маршрутов команд</h2>
          <p className="section-block__text">
            Здесь ты собираешь маршруты по слотам. Одна и та же загадка не может повторяться в разных маршрутах одной игры.
          </p>
        </div>
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
          Во всех маршрутах одной игры должно быть одинаковое количество заданий.
        </p>
      ) : null}

      <form className="auth-form" onSubmit={handleCreateRoute}>
        <div className="split-grid split-grid--triple">
          <label className="field">
            <span>Слот маршрута</span>
            <select name="slotNumber" value={routeForm.slotNumber} onChange={handleRouteFormChange} disabled={!canManageContent}>
              {slotNumbers.map((slotNumber) => (
                <option key={slotNumber} value={slotNumber}>
                  Маршрут {slotNumber}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Название маршрута</span>
            <input name="name" value={routeForm.name} onChange={handleRouteFormChange} disabled={!canManageContent} />
          </label>
          <label className="field">
            <span>Позиция задания</span>
            <input name="orderIndex" type="number" min="1" value={routeForm.orderIndex} onChange={handleRouteFormChange} disabled={!canManageContent} />
          </label>
        </div>

        <label className="field">
          <span>Добавить задание</span>
          <select name="taskId" value={routeForm.taskId} onChange={handleRouteFormChange} disabled={!canManageContent || availableRouteTasks.length === 0}>
            <option value="">Пока без задания</option>
            {availableRouteTasks.map((task) => (
              <option key={task.id} value={task.id}>
                {task.title}
              </option>
            ))}
          </select>
        </label>

        <button className="button button--primary" type="submit" disabled={!canManageContent || isCreatingRoute || isAddingRouteItem}>
          {isCreatingRoute || isAddingRouteItem ? 'Сохраняем...' : 'Сохранить маршрут'}
        </button>
      </form>

      {isFetchingTasks || isFetchingRoutes ? <p className="page-note">Загрузка маршрутов...</p> : null}

      {slotNumbers.length === 0 ? (
        <section className="empty-state">
          <h2>Количество маршрутов не задано</h2>
          <p>Сначала укажи количество маршрутов в параметрах игры.</p>
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
                  <p>{route ? `Заданий: ${route.items.length}` : 'Маршрут ещё не создан'}</p>
                  {route?.assignedTeamName ? <p>Назначен команде: {route.assignedTeamName}</p> : null}
                </div>

                {route ? (
                  editingRouteId === route.id ? (
                    <div className="list-card__actions">
                      <input value={routeEditName} onChange={(event) => setRouteEditName(event.target.value)} />
                      <button className="button button--primary" type="button" onClick={() => handleUpdateRoute(route.id)} disabled={!canManageContent || isUpdatingRoute}>
                        Сохранить
                      </button>
                      <button className="button button--secondary" type="button" onClick={cancelRouteEditing}>
                        Отмена
                      </button>
                    </div>
                  ) : (
                    <div className="list-card__actions">
                      <button className="button button--secondary" type="button" onClick={() => startRouteEditing(route)} disabled={!canManageContent}>
                        Переименовать
                      </button>
                      <button className="button button--secondary" type="button" onClick={() => handleDeleteRoute(route.id)} disabled={!canManageContent || isDeletingRoute || Boolean(route.assignedTeamId)}>
                        Удалить
                      </button>
                    </div>
                  )
                ) : null}
              </div>

              {route ? (
                route.items.length > 0 ? (
                  <div className="route-editor__items">
                    {route.items.map((item) => (
                      <div key={item.id} className="route-editor__item">
                        <div>
                          <strong>{item.orderIndex}. {item.taskTitle}</strong>
                        </div>
                        <button className="button button--secondary" type="button" onClick={() => handleRemoveTaskFromRoute(route.id, item.id)} disabled={!canManageContent || isRemovingRouteItem}>
                          Убрать
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="route-editor__empty">
                    <p>В этот маршрут пока не добавлены задания.</p>
                  </div>
                )
              ) : (
                <div className="route-editor__empty">
                  <p>Для этого слота маршрут ещё не создан.</p>
                </div>
              )}
            </article>
          )
        })}
      </div>
    </section>
  )
}
