import { useMemo, useState } from 'react'
import { useOutletContext, useParams } from 'react-router-dom'
import { useCancelOrganizerGameMutation, useUpdateGameMutation } from '../features/game/gameApi.js'
import { formatGameStatus, toDatetimeLocalValue } from '../shared/lib/formatters.js'

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

export function OrganizerGameEditPage() {
  const { gameId } = useParams()
  const { game, isGameEditable, canCancelGame, hasStartedByTime } = useOutletContext()
  const [updateGame, { isLoading: isSavingGame }] = useUpdateGameMutation()
  const [cancelOrganizerGame, { isLoading: isCancelingGame }] = useCancelOrganizerGameMutation()
  const [gameDraft, setGameDraft] = useState(null)
  const [gameError, setGameError] = useState('')
  const [gameMessage, setGameMessage] = useState('')

  const gameForm = gameDraft || buildGameForm(game)

  const restrictionsMessage = useMemo(() => {
    if (!game) {
      return ''
    }

    if (game.status === 'CANCELED') {
      return 'Игра отменена. Редактирование больше недоступно.'
    }

    if (game.status === 'IN_PROGRESS') {
      return 'Игра уже запущена. Изменить параметры больше нельзя.'
    }

    if (game.status === 'FINISHED') {
      return 'Игра завершена. Параметры зафиксированы.'
    }

    if (hasStartedByTime) {
      return 'Время старта уже наступило. Изменение параметров недоступно.'
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

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Создание и редактирование игры</h2>
          <p className="section-block__text">Статус: {formatGameStatus(game.status)}</p>
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
            <span>Мин. размер команды</span>
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
            <span>Макс. размер команды</span>
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
            <span>Штраф за провал, мин</span>
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
          <button className="button button--primary" type="submit" disabled={!isGameEditable || isSavingGame}>
            {isSavingGame ? 'Сохраняем...' : 'Сохранить изменения'}
          </button>

          <button
            className="button button--secondary"
            type="button"
            onClick={handleCancelGame}
            disabled={!canCancelGame || isCancelingGame}
          >
            {isCancelingGame ? 'Отменяем игру...' : 'Отменить игру'}
          </button>
        </div>
      </form>
    </section>
  )
}
