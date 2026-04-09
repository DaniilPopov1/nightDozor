import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  useAddTaskToRouteMutation,
  useApproveRegistrationMutation,
  useCancelOrganizerGameMutation,
  useCreateRouteMutation,
  useCreateTaskHintMutation,
  useCreateTaskMutation,
  useGetIncomingRegistrationsQuery,
  useGetOrganizerGameByIdQuery,
  useGetOrganizerGameRoutesQuery,
  useGetOrganizerGameTasksQuery,
  useRejectRegistrationMutation,
  useUpdateGameMutation,
} from '../features/game/gameApi.js'
import {
  formatDateTime,
  formatGameStatus,
  formatRegistrationStatus,
  toDatetimeLocalValue,
} from '../shared/lib/formatters.js'

function buildGameForm(game) {
  return {
    title: game?.title || '',
    description: game?.description || '',
    city: game?.city || '',
    minTeamSize: String(game?.minTeamSize || 1),
    maxTeamSize: String(game?.maxTeamSize || 1),
    routeSlotsCount: String(game?.routeSlotsCount || 1),
    taskFailurePenaltyMinutes: String(game?.taskFailurePenaltyMinutes || 0),
    registrationStartsAt: toDatetimeLocalValue(game?.registrationStartsAt),
    registrationEndsAt: toDatetimeLocalValue(game?.registrationEndsAt),
    startsAt: toDatetimeLocalValue(game?.startsAt),
  }
}

export function OrganizerGameDetailsPage() {
  const { gameId } = useParams()
  const { data: game, isFetching, error } = useGetOrganizerGameByIdQuery(gameId)
  const { data: incomingRegistrations = [] } = useGetIncomingRegistrationsQuery(gameId)
  const {
    data: tasks = [],
    isFetching: isFetchingTasks,
    error: tasksLoadError,
  } = useGetOrganizerGameTasksQuery(gameId)
  const {
    data: routes = [],
    isFetching: isFetchingRoutes,
    error: routesLoadError,
  } = useGetOrganizerGameRoutesQuery(gameId)
  const [updateGame, { isLoading: isSavingGame }] = useUpdateGameMutation()
  const [cancelOrganizerGame, { isLoading: isCancelingGame }] = useCancelOrganizerGameMutation()
  const [approveRegistration] = useApproveRegistrationMutation()
  const [rejectRegistration] = useRejectRegistrationMutation()
  const [createTask, { isLoading: isCreatingTask }] = useCreateTaskMutation()
  const [createTaskHint, { isLoading: isCreatingHint }] = useCreateTaskHintMutation()
  const [createRoute, { isLoading: isCreatingRoute }] = useCreateRouteMutation()
  const [addTaskToRoute, { isLoading: isAddingRouteItem }] = useAddTaskToRouteMutation()
  const [gameDraft, setGameDraft] = useState(null)
  const [gameError, setGameError] = useState('')
  const [gameMessage, setGameMessage] = useState('')
  const [taskMessage, setTaskMessage] = useState('')
  const [taskError, setTaskError] = useState('')
  const [registrationMessage, setRegistrationMessage] = useState('')
  const [registrationError, setRegistrationError] = useState('')
  const [taskForm, setTaskForm] = useState({
    title: '',
    riddleText: '',
    answerKey: '',
    orderIndex: '1',
    timeLimitMinutes: '60',
    failurePenaltyMinutes: '10',
    hintOneText: '',
    hintOneDelay: '15',
    hintTwoText: '',
    hintTwoDelay: '15',
  })
  const [routeForm, setRouteForm] = useState({
    slotNumber: '1',
    name: '',
    taskId: '',
    orderIndex: '1',
  })
  const [pageOpenedAt] = useState(() => Date.now())

  const slotNumbers = useMemo(
    () => Array.from({ length: game?.routeSlotsCount || 0 }, (_, index) => index + 1),
    [game?.routeSlotsCount],
  )
  const routesBySlot = useMemo(
    () => new Map(routes.map((route) => [String(route.slotNumber), route])),
    [routes],
  )
  const assignedTaskIds = useMemo(
    () =>
      new Set(
        routes.flatMap((route) => route.items.map((item) => String(item.taskId))),
      ),
    [routes],
  )
  const availableRouteTasks = useMemo(
    () => tasks.filter((task) => !assignedTaskIds.has(String(task.id))),
    [assignedTaskIds, tasks],
  )
  const gameForm = gameDraft || buildGameForm(game)
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
  const canReviewRegistrations = useMemo(() => {
    if (!game) {
      return false
    }

    return !['IN_PROGRESS', 'FINISHED', 'CANCELED'].includes(game.status)
  }, [game])
  const canCancelGame = game && !['FINISHED', 'CANCELED'].includes(game.status)

  const restrictionsMessage = useMemo(() => {
    if (!game) {
      return ''
    }

    if (game.status === 'CANCELED') {
      return 'Игра отменена. Редактирование и настройка больше недоступны.'
    }

    if (game.status === 'IN_PROGRESS') {
      return 'Игра уже запущена. Изменение параметров и настройка сценария недоступны.'
    }

    if (game.status === 'FINISHED') {
      return 'Игра завершена. Параметры и сценарий больше нельзя изменять.'
    }

    if (hasStartedByTime) {
      return 'Время старта уже наступило. Редактирование игры недоступно.'
    }

    return ''
  }, [game, hasStartedByTime])

  const handleGameFormChange = (event) => {
    const { name, value } = event.target
    setGameDraft((current) => ({
      ...(current || buildGameForm(game)),
      [name]: value,
    }))
  }

  const handleTaskFormChange = (event) => {
    const { name, value } = event.target
    setTaskForm((current) => ({ ...current, [name]: value }))
  }

  const handleRouteFormChange = (event) => {
    const { name, value } = event.target
    setRouteForm((current) => ({ ...current, [name]: value }))
  }

  const handleSaveGame = async (event) => {
    event.preventDefault()

    if (!gameForm.title.trim() || !gameForm.description.trim() || !gameForm.city.trim()) {
      setGameError('Заполни название, описание и город игры')
      return
    }

    if (Number(gameForm.minTeamSize) > Number(gameForm.maxTeamSize)) {
      setGameError('Минимальный размер команды не может быть больше максимального')
      return
    }

    if (Number(gameForm.routeSlotsCount) < 1) {
      setGameError('Количество маршрутов должно быть не меньше 1')
      return
    }

    setGameError('')
    setGameMessage('')

    try {
      await updateGame({
        gameId,
        payload: {
          title: gameForm.title.trim(),
          description: gameForm.description.trim(),
          city: gameForm.city.trim(),
          minTeamSize: Number(gameForm.minTeamSize),
          maxTeamSize: Number(gameForm.maxTeamSize),
          routeSlotsCount: Number(gameForm.routeSlotsCount),
          taskFailurePenaltyMinutes: Number(gameForm.taskFailurePenaltyMinutes),
          registrationStartsAt: gameForm.registrationStartsAt
            ? new Date(gameForm.registrationStartsAt).toISOString()
            : null,
          registrationEndsAt: gameForm.registrationEndsAt
            ? new Date(gameForm.registrationEndsAt).toISOString()
            : null,
          startsAt: new Date(gameForm.startsAt).toISOString(),
        },
      }).unwrap()
      setGameDraft(null)
      setGameMessage('Параметры игры сохранены')
    } catch (requestError) {
      setGameError(requestError?.message || 'Не удалось сохранить игру')
    }
  }

  const handleCancelGame = async () => {
    setGameError('')
    setGameMessage('')

    try {
      await cancelOrganizerGame(gameId).unwrap()
      setGameMessage('Игра отменена')
    } catch (requestError) {
      setGameError(requestError?.message || 'Не удалось отменить игру')
    }
  }

  const handleRegistrationDecision = async (registrationId, decision) => {
    setRegistrationError('')
    setRegistrationMessage('')

    try {
      if (decision === 'approve') {
        await approveRegistration({ gameId, registrationId }).unwrap()
        setRegistrationMessage('Заявка команды подтверждена, маршрут назначен автоматически')
      } else {
        await rejectRegistration({ gameId, registrationId }).unwrap()
        setRegistrationMessage('Заявка команды отклонена')
      }
    } catch (requestError) {
      setRegistrationError(requestError?.message || 'Не удалось обработать заявку')
    }
  }

  const handleCreateTask = async (event) => {
    event.preventDefault()
    setTaskError('')
    setTaskMessage('')

    try {
      const createdTask = await createTask({
        gameId,
        payload: {
          title: taskForm.title.trim(),
          riddleText: taskForm.riddleText.trim(),
          answerKey: taskForm.answerKey.trim(),
          orderIndex: Number(taskForm.orderIndex),
          timeLimitMinutes: Number(taskForm.timeLimitMinutes),
          failurePenaltyMinutes: Number(taskForm.failurePenaltyMinutes),
        },
      }).unwrap()

      if (taskForm.hintOneText.trim()) {
        await createTaskHint({
          gameId,
          taskId: createdTask.id,
          payload: {
            text: taskForm.hintOneText.trim(),
            orderIndex: 1,
            delayMinutesFromPreviousHint: Number(taskForm.hintOneDelay),
          },
        }).unwrap()
      }

      if (taskForm.hintTwoText.trim()) {
        await createTaskHint({
          gameId,
          taskId: createdTask.id,
          payload: {
            text: taskForm.hintTwoText.trim(),
            orderIndex: 2,
            delayMinutesFromPreviousHint: Number(taskForm.hintTwoDelay),
          },
        }).unwrap()
      }

      setTaskMessage('Задание и подсказки сохранены')
      setTaskForm({
        title: '',
        riddleText: '',
        answerKey: '',
        orderIndex: String(tasks.length + 2),
        timeLimitMinutes: '60',
        failurePenaltyMinutes: '10',
        hintOneText: '',
        hintOneDelay: '15',
        hintTwoText: '',
        hintTwoDelay: '15',
      })
    } catch (requestError) {
      setTaskError(requestError?.message || 'Не удалось создать задание')
    }
  }

  const handleCreateRoute = async (event) => {
    event.preventDefault()
    setTaskError('')
    setTaskMessage('')

    if (!routeForm.slotNumber || !routeForm.name.trim()) {
      setTaskError('Для маршрута выбери номер слота и укажи название')
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

      setTaskMessage('Маршрут сохранён')
      setRouteForm((current) => ({
        ...current,
        taskId: '',
        orderIndex: String(Number(current.orderIndex) + 1),
      }))
    } catch (requestError) {
      setTaskError(requestError?.message || 'Не удалось сохранить маршрут')
    }
  }

  return (
    <section className="page-card">
      <div className="page-card__header page-card__header--row">
        <div>
          <p className="page-card__eyebrow">Организатор</p>
          <h1>{game?.title || 'Карточка игры'}</h1>
          <p className="page-card__text">
            Здесь можно управлять параметрами игры, входящими заявками, заданиями,
            маршрутами и запуском игры.
          </p>
        </div>

        <Link className="button button--secondary" to="/organizer/games">
          Ко всем играм
        </Link>
      </div>

      {error?.message ? <p className="form-message form-message--error">{error.message}</p> : null}
      {isFetching ? <p className="page-note">Загрузка карточки игры...</p> : null}

      {game ? (
        <div className="stack">
          <section className="section-block">
            <div className="section-block__header">
              <div>
                <h2>Параметры игры</h2>
                <p className="section-block__text">
                  Статус: {formatGameStatus(game.status)}
                </p>
              </div>
            </div>

            {gameError ? <p className="form-message form-message--error">{gameError}</p> : null}
            {gameMessage ? <p className="form-message form-message--success">{gameMessage}</p> : null}
            {restrictionsMessage ? (
              <p className="form-message form-message--error">{restrictionsMessage}</p>
            ) : null}

            <form className="auth-form" onSubmit={handleSaveGame}>
              <div className="split-grid">
                <label className="field">
                  <span>Название</span>
                  <input
                    name="title"
                    value={gameForm.title}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
                <label className="field">
                  <span>Город</span>
                  <input
                    name="city"
                    value={gameForm.city}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
              </div>

              <label className="field">
                <span>Описание</span>
                <textarea
                  className="field__textarea"
                  name="description"
                  value={gameForm.description}
                  onChange={handleGameFormChange}
                  rows="5"
                  disabled={!isGameEditable}
                />
              </label>

              <div className="split-grid split-grid--triple">
                <label className="field">
                  <span>Мин. размер</span>
                  <input
                    name="minTeamSize"
                    type="number"
                    min="1"
                    value={gameForm.minTeamSize}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
                <label className="field">
                  <span>Макс. размер</span>
                  <input
                    name="maxTeamSize"
                    type="number"
                    min="1"
                    value={gameForm.maxTeamSize}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
                <label className="field">
                  <span>Количество маршрутов</span>
                  <input
                    name="routeSlotsCount"
                    type="number"
                    min="1"
                    value={gameForm.routeSlotsCount}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
              </div>

              <div className="split-grid">
                <label className="field">
                  <span>Штраф, мин</span>
                  <input
                    name="taskFailurePenaltyMinutes"
                    type="number"
                    min="0"
                    value={gameForm.taskFailurePenaltyMinutes}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
              </div>

              <div className="split-grid">
                <label className="field">
                  <span>Начало регистрации</span>
                  <input
                    name="registrationStartsAt"
                    type="datetime-local"
                    value={gameForm.registrationStartsAt}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
                <label className="field">
                  <span>Конец регистрации</span>
                  <input
                    name="registrationEndsAt"
                    type="datetime-local"
                    value={gameForm.registrationEndsAt}
                    onChange={handleGameFormChange}
                    disabled={!isGameEditable}
                  />
                </label>
              </div>

              <label className="field">
                <span>Старт игры</span>
                <input
                  name="startsAt"
                  type="datetime-local"
                  value={gameForm.startsAt}
                  onChange={handleGameFormChange}
                  disabled={!isGameEditable}
                />
              </label>

              <div className="cta-group">
                <button
                  className="button button--primary"
                  type="submit"
                  disabled={isSavingGame || !isGameEditable}
                >
                  {isSavingGame ? 'Сохраняем...' : 'Сохранить изменения'}
                </button>
              </div>
            </form>

            <div className="cta-group">
              <button
                className="button button--secondary"
                type="button"
                onClick={handleCancelGame}
                disabled={isCancelingGame || !canCancelGame}
              >
                {isCancelingGame ? 'Отменяем...' : 'Отменить игру'}
              </button>
            </div>

            <dl className="detail-grid detail-grid--spaced">
              <div>
                <dt>Начало регистрации</dt>
                <dd>{formatDateTime(game.registrationStartsAt)}</dd>
              </div>
              <div>
                <dt>Конец регистрации</dt>
                <dd>{formatDateTime(game.registrationEndsAt)}</dd>
              </div>
              <div>
                <dt>Старт игры</dt>
                <dd>{formatDateTime(game.startsAt)}</dd>
              </div>
              <div>
                <dt>Маршрутов в игре</dt>
                <dd>{game.routeSlotsCount}</dd>
              </div>
              <div>
                <dt>Организатор</dt>
                <dd>{game.organizerEmail}</dd>
              </div>
            </dl>
          </section>

          <section className="section-block">
            <h2>Входящие заявки команд</h2>
            {registrationError ? (
              <p className="form-message form-message--error">{registrationError}</p>
            ) : null}
            {registrationMessage ? (
              <p className="form-message form-message--success">{registrationMessage}</p>
            ) : null}
            {!canReviewRegistrations ? (
              <p className="page-note">
                После отмены, старта или завершения игры обработка заявок недоступна.
              </p>
            ) : null}

            {incomingRegistrations.length === 0 ? (
              <p className="page-note">Пока нет заявок на участие в этой игре.</p>
            ) : (
              <div className="list-grid">
                {incomingRegistrations.map((registration) => (
                  <article key={registration.registrationId} className="list-card">
                    <h3>{registration.teamName}</h3>
                    <p>Город: {registration.teamCity}</p>
                    <p>Капитан: {registration.captainEmail}</p>
                    <p>Участников: {registration.activeMembersCount}</p>
                    <p>Статус: {formatRegistrationStatus(registration.status)}</p>
                    <p>Подана: {formatDateTime(registration.createdAt)}</p>

                    <div className="list-card__actions">
                      <button
                        className="button button--primary"
                        type="button"
                        onClick={() =>
                          handleRegistrationDecision(registration.registrationId, 'approve')
                        }
                        disabled={!canReviewRegistrations || registration.status === 'APPROVED'}
                      >
                        Подтвердить
                      </button>
                      <button
                        className="button button--secondary"
                        type="button"
                        onClick={() =>
                          handleRegistrationDecision(registration.registrationId, 'reject')
                        }
                        disabled={!canReviewRegistrations || registration.status === 'REJECTED'}
                      >
                        Отклонить
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="section-block">
            <h2>Создание задания</h2>
            <p className="section-block__hint">
              Ниже показывается актуальный список заданий этой игры, загруженный с сервера.
            </p>
            {!canManageContent ? (
              <p className="page-note">
                После старта, завершения или отмены игры создавать новые задания нельзя.
              </p>
            ) : null}

            {taskError ? <p className="form-message form-message--error">{taskError}</p> : null}
            {taskMessage ? <p className="form-message form-message--success">{taskMessage}</p> : null}
            {tasksLoadError?.message ? (
              <p className="form-message form-message--error">{tasksLoadError.message}</p>
            ) : null}
            {isFetchingTasks ? <p className="page-note">Загружаем задания игры...</p> : null}

            <form className="auth-form" onSubmit={handleCreateTask}>
              <div className="split-grid">
                <label className="field">
                  <span>Название задания</span>
                  <input
                    name="title"
                    value={taskForm.title}
                    onChange={handleTaskFormChange}
                    disabled={!canManageContent}
                  />
                </label>
                <label className="field">
                  <span>Порядковый номер</span>
                  <input
                    name="orderIndex"
                    type="number"
                    min="1"
                    value={taskForm.orderIndex}
                    onChange={handleTaskFormChange}
                    disabled={!canManageContent}
                  />
                </label>
              </div>

              <label className="field">
                <span>Текст загадки</span>
                <textarea
                  className="field__textarea"
                  name="riddleText"
                  value={taskForm.riddleText}
                  onChange={handleTaskFormChange}
                  rows="5"
                  disabled={!canManageContent}
                />
              </label>

              <div className="split-grid split-grid--triple">
                <label className="field">
                  <span>Ключ</span>
                  <input
                    name="answerKey"
                    value={taskForm.answerKey}
                    onChange={handleTaskFormChange}
                    disabled={!canManageContent}
                  />
                </label>
                <label className="field">
                  <span>Лимит времени, мин</span>
                  <input
                    name="timeLimitMinutes"
                    type="number"
                    min="1"
                    value={taskForm.timeLimitMinutes}
                    onChange={handleTaskFormChange}
                    disabled={!canManageContent}
                  />
                </label>
                <label className="field">
                  <span>Штраф, мин</span>
                  <input
                    name="failurePenaltyMinutes"
                    type="number"
                    min="0"
                    value={taskForm.failurePenaltyMinutes}
                    onChange={handleTaskFormChange}
                    disabled={!canManageContent}
                  />
                </label>
              </div>

              <div className="split-grid">
                <label className="field">
                  <span>Первая подсказка</span>
                  <textarea
                    className="field__textarea field__textarea--small"
                    name="hintOneText"
                    value={taskForm.hintOneText}
                    onChange={handleTaskFormChange}
                    rows="3"
                    disabled={!canManageContent}
                  />
                </label>
                <label className="field">
                  <span>Задержка первой подсказки, мин</span>
                  <input
                    name="hintOneDelay"
                    type="number"
                    min="0"
                    value={taskForm.hintOneDelay}
                    onChange={handleTaskFormChange}
                    disabled={!canManageContent}
                  />
                </label>
              </div>

              <div className="split-grid">
                <label className="field">
                  <span>Вторая подсказка</span>
                  <textarea
                    className="field__textarea field__textarea--small"
                    name="hintTwoText"
                    value={taskForm.hintTwoText}
                    onChange={handleTaskFormChange}
                    rows="3"
                    disabled={!canManageContent}
                  />
                </label>
                <label className="field">
                  <span>Задержка второй подсказки, мин</span>
                  <input
                    name="hintTwoDelay"
                    type="number"
                    min="0"
                    value={taskForm.hintTwoDelay}
                    onChange={handleTaskFormChange}
                    disabled={!canManageContent}
                  />
                </label>
              </div>

              <button
                className="button button--primary"
                type="submit"
                disabled={!canManageContent || isCreatingTask || isCreatingHint}
              >
                {isCreatingTask || isCreatingHint ? 'Сохраняем задание...' : 'Создать задание'}
              </button>
            </form>

            {tasks.length > 0 ? (
              <div className="list-grid">
                {tasks.map((task) => (
                  <article key={task.id} className="list-card">
                    <h3>{task.title}</h3>
                    <p>Порядок: {task.orderIndex}</p>
                    <p>Лимит времени: {task.timeLimitMinutes} мин.</p>
                    <p>Штраф: {task.failurePenaltyMinutes} мин.</p>
                    <p>Подсказок: {task.hints?.length || 0}</p>
                  </article>
                ))}
              </div>
            ) : (
              <p className="page-note">Задания ещё не созданы.</p>
            )}
          </section>

          <section className="section-block">
            <h2>Маршруты игры</h2>
            <p className="section-block__hint">
              Организатор заранее готовит маршруты по слотам. Когда заявка команды
              подтверждается, ей случайно назначается один из свободных маршрутов.
            </p>
            <p className="section-block__hint">
              Одно и то же задание можно включить только в один маршрут этой игры.
            </p>
            {!canManageContent ? (
              <p className="page-note">
                После старта, завершения или отмены игры создание и настройка маршрутов недоступны.
              </p>
            ) : null}
            {routesLoadError?.message ? (
              <p className="form-message form-message--error">{routesLoadError.message}</p>
            ) : null}
            {isFetchingRoutes ? <p className="page-note">Загружаем маршруты игры...</p> : null}

            <form className="auth-form" onSubmit={handleCreateRoute}>
              <div className="split-grid">
                <label className="field">
                  <span>Номер маршрута</span>
                  <select
                    name="slotNumber"
                    value={routeForm.slotNumber}
                    onChange={handleRouteFormChange}
                    disabled={!canManageContent}
                  >
                    <option value="">Выбери маршрут</option>
                    {slotNumbers.map((slotNumber) => (
                      <option key={slotNumber} value={slotNumber}>
                        Маршрут {slotNumber}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Название маршрута</span>
                  <input
                    name="name"
                    value={routeForm.name}
                    onChange={handleRouteFormChange}
                    disabled={!canManageContent}
                  />
                </label>
              </div>

              <div className="split-grid">
                <label className="field">
                  <span>Добавить задание</span>
                  <select
                    name="taskId"
                    value={routeForm.taskId}
                    onChange={handleRouteFormChange}
                    disabled={!canManageContent}
                  >
                    <option value="">Пока без задания</option>
                    {availableRouteTasks.map((task) => (
                      <option key={task.id} value={task.id}>
                        {task.orderIndex}. {task.title}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Порядок в маршруте</span>
                  <input
                    name="orderIndex"
                    type="number"
                    min="1"
                    value={routeForm.orderIndex}
                    onChange={handleRouteFormChange}
                    disabled={!canManageContent}
                  />
                </label>
              </div>

              <button
                className="button button--primary"
                type="submit"
                disabled={!canManageContent || isCreatingRoute || isAddingRouteItem}
              >
                {isCreatingRoute || isAddingRouteItem ? 'Сохраняем маршрут...' : 'Сохранить маршрут'}
              </button>
            </form>

            {slotNumbers.length > 0 ? (
              <div className="list-grid">
                {slotNumbers.map((slotNumber) => {
                  const route = routesBySlot.get(String(slotNumber))

                  return (
                    <article key={slotNumber} className="list-card">
                      <h3>Маршрут {slotNumber}</h3>
                      {route ? (
                        <>
                          <p>Название: {route.name}</p>
                          <p>
                            Назначен команде:{' '}
                            {route.assignedTeamName || 'Пока свободен'}
                          </p>
                          <p>Заданий в маршруте: {route.items.length}</p>
                          {route.items.map((item) => (
                            <p key={item.id}>
                              {item.orderIndex}. {item.taskTitle}
                            </p>
                          ))}
                        </>
                      ) : (
                        <p>Маршрут для этого слота ещё не создан.</p>
                      )}
                    </article>
                  )
                })}
              </div>
            ) : (
              <p className="page-note">Для этой игры пока не задано количество маршрутов.</p>
            )}
          </section>
        </div>
      ) : null}
    </section>
  )
}
