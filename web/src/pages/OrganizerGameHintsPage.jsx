import { useMemo, useState } from 'react'
import { useOutletContext, useParams } from 'react-router-dom'
import {
  useCreateTaskHintMutation,
  useDeleteTaskHintMutation,
  useGetOrganizerGameTasksQuery,
  useUpdateTaskHintMutation,
} from '../features/game/gameApi.js'

function buildHintDrafts(tasks) {
  return tasks.reduce((accumulator, task) => {
    const hasFirstHint = task.hints.some((hint) => hint.orderIndex === 1)
    const hasSecondHint = task.hints.some((hint) => hint.orderIndex === 2)

    accumulator[task.id] = {
      hintOneText: '',
      hintOneDelay: '15',
      hintTwoText: '',
      hintTwoDelay: '15',
      hasFirstHint,
      hasSecondHint,
    }

    return accumulator
  }, {})
}

export function OrganizerGameHintsPage() {
  const { gameId } = useParams()
  const { canManageContent } = useOutletContext()
  const { data: tasks = [], isFetching, error } = useGetOrganizerGameTasksQuery(gameId)
  const [createTaskHint, { isLoading }] = useCreateTaskHintMutation()
  const [updateTaskHint, { isLoading: isUpdatingHint }] = useUpdateTaskHintMutation()
  const [deleteTaskHint, { isLoading: isDeletingHint }] = useDeleteTaskHintMutation()
  const [hintsState, setHintsState] = useState({})
  const [editingHintId, setEditingHintId] = useState(null)
  const [editingTaskId, setEditingTaskId] = useState(null)
  const [hintEditForm, setHintEditForm] = useState({
    text: '',
    orderIndex: '1',
    delayMinutesFromPreviousHint: '15',
  })
  const [message, setMessage] = useState('')
  const [requestError, setRequestError] = useState('')

  const hintDrafts = useMemo(() => {
    const baseDrafts = buildHintDrafts(tasks)

    return Object.entries(baseDrafts).reduce((accumulator, [taskId, draft]) => {
      accumulator[taskId] = {
        ...draft,
        ...(hintsState[taskId] || {}),
      }

      return accumulator
    }, {})
  }, [hintsState, tasks])

  const handleChange = (taskId, field, value) => {
    setHintsState((current) => ({
      ...current,
      [taskId]: {
        ...(current[taskId] || buildHintDrafts(tasks)[taskId]),
        [field]: value,
      },
    }))
  }

  const handleSaveHints = async (task) => {
    const draft = hintDrafts[task.id]
    setRequestError('')
    setMessage('')

    try {
      if (!draft.hasFirstHint && draft.hintOneText.trim()) {
        await createTaskHint({
          gameId,
          taskId: task.id,
          payload: {
            text: draft.hintOneText.trim(),
            orderIndex: 1,
            delayMinutesFromPreviousHint: Number(draft.hintOneDelay),
          },
        }).unwrap()
      }

      if (!draft.hasSecondHint && draft.hintTwoText.trim()) {
        await createTaskHint({
          gameId,
          taskId: task.id,
          payload: {
            text: draft.hintTwoText.trim(),
            orderIndex: 2,
            delayMinutesFromPreviousHint: Number(draft.hintTwoDelay),
          },
        }).unwrap()
      }

      setMessage('Подсказки сохранены')
      setHintsState({})
    } catch (hintError) {
      setRequestError(hintError?.message || 'Не удалось сохранить подсказки')
    }
  }

  const startHintEditing = (taskId, hint) => {
    setEditingTaskId(taskId)
    setEditingHintId(hint.id)
    setHintEditForm({
      text: hint.text,
      orderIndex: String(hint.orderIndex),
      delayMinutesFromPreviousHint: String(hint.delayMinutesFromPreviousHint),
    })
  }

  const cancelHintEditing = () => {
    setEditingTaskId(null)
    setEditingHintId(null)
    setHintEditForm({
      text: '',
      orderIndex: '1',
      delayMinutesFromPreviousHint: '15',
    })
  }

  const handleHintEditFormChange = (event) => {
    const { name, value } = event.target
    setHintEditForm((current) => ({ ...current, [name]: value }))
  }

  const handleUpdateHint = async (taskId, hintId) => {
    setRequestError('')
    setMessage('')

    try {
      await updateTaskHint({
        gameId,
        taskId,
        hintId,
        payload: {
          text: hintEditForm.text.trim(),
          orderIndex: Number(hintEditForm.orderIndex),
          delayMinutesFromPreviousHint: Number(hintEditForm.delayMinutesFromPreviousHint),
        },
      }).unwrap()

      setMessage('Подсказка обновлена')
      cancelHintEditing()
    } catch (hintError) {
      setRequestError(hintError?.message || 'Не удалось обновить подсказку')
    }
  }

  const handleDeleteHint = async (taskId, hintId) => {
    setRequestError('')
    setMessage('')

    try {
      await deleteTaskHint({ gameId, taskId, hintId }).unwrap()
      setMessage('Подсказка удалена')

      if (editingHintId === hintId) {
        cancelHintEditing()
      }
    } catch (hintError) {
      setRequestError(hintError?.message || 'Не удалось удалить подсказку')
    }
  }

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Экран управления подсказками</h2>
          <p className="section-block__text">
            Для каждого задания можно задать до двух подсказок и интервалы их появления.
          </p>
        </div>
      </div>

      {error?.message ? <p className="form-message form-message--error">{error.message}</p> : null}
      {requestError ? <p className="form-message form-message--error">{requestError}</p> : null}
      {message ? <p className="form-message form-message--success">{message}</p> : null}
      {!canManageContent ? (
        <p className="form-message form-message--error">
          После старта или отмены игры подсказки больше нельзя менять.
        </p>
      ) : null}

      {isFetching ? <p className="page-note">Загрузка заданий...</p> : null}

      {tasks.length === 0 && !isFetching ? (
        <section className="empty-state">
          <h2>Сначала создай задания</h2>
          <p>Подсказки настраиваются отдельно для каждой загадки после её создания.</p>
        </section>
      ) : null}

      <div className="list-grid list-grid--balanced">
        {tasks.map((task) => {
          const draft = hintDrafts[task.id]
          const sortedHints = [...task.hints].sort((a, b) => a.orderIndex - b.orderIndex)

          return (
            <article key={task.id} className="list-card">
              <h3>{task.title}</h3>
              <p>Подсказок настроено: {task.hints.length} из 2</p>

              {sortedHints.length > 0 ? (
                <div className="profile-list">
                  {sortedHints.map((hint) => (
                    <div key={hint.id}>
                      {editingHintId === hint.id && editingTaskId === task.id ? (
                        <div className="stack">
                          <label className="field">
                            <span>Текст подсказки</span>
                            <textarea
                              className="field__textarea field__textarea--small"
                              rows="3"
                              name="text"
                              value={hintEditForm.text}
                              onChange={handleHintEditFormChange}
                            />
                          </label>
                          <div className="split-grid">
                            <label className="field">
                              <span>Порядок</span>
                              <select
                                name="orderIndex"
                                value={hintEditForm.orderIndex}
                                onChange={handleHintEditFormChange}
                              >
                                <option value="1">Подсказка 1</option>
                                <option value="2">Подсказка 2</option>
                              </select>
                            </label>
                            <label className="field">
                              <span>Задержка, мин</span>
                              <input
                                name="delayMinutesFromPreviousHint"
                                type="number"
                                min="0"
                                value={hintEditForm.delayMinutesFromPreviousHint}
                                onChange={handleHintEditFormChange}
                              />
                            </label>
                          </div>
                          <div className="list-card__actions">
                            <button
                              className="button button--primary"
                              type="button"
                              onClick={() => handleUpdateHint(task.id, hint.id)}
                              disabled={!canManageContent || isUpdatingHint}
                            >
                              Сохранить
                            </button>
                            <button
                              className="button button--secondary"
                              type="button"
                              onClick={cancelHintEditing}
                            >
                              Отмена
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <dt>Подсказка {hint.orderIndex}</dt>
                          <dd>{hint.text}</dd>
                          <p className="page-note">
                            Появляется через {hint.delayMinutesFromPreviousHint} мин
                          </p>
                          <div className="list-card__actions">
                            <button
                              className="button button--secondary"
                              type="button"
                              onClick={() => startHintEditing(task.id, hint)}
                              disabled={!canManageContent}
                            >
                              Редактировать
                            </button>
                            <button
                              className="button button--secondary"
                              type="button"
                              onClick={() => handleDeleteHint(task.id, hint.id)}
                              disabled={!canManageContent || isDeletingHint}
                            >
                              Удалить
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p>Для этого задания подсказки ещё не заданы.</p>
              )}

              {!draft.hasFirstHint ? (
                <div className="stack">
                  <label className="field">
                    <span>Первая подсказка</span>
                    <textarea
                      className="field__textarea field__textarea--small"
                      rows="3"
                      value={draft.hintOneText}
                      onChange={(event) => handleChange(task.id, 'hintOneText', event.target.value)}
                      disabled={!canManageContent}
                    />
                  </label>
                  <label className="field">
                    <span>Пауза до первой подсказки, мин</span>
                    <input
                      type="number"
                      min="0"
                      value={draft.hintOneDelay}
                      onChange={(event) => handleChange(task.id, 'hintOneDelay', event.target.value)}
                      disabled={!canManageContent}
                    />
                  </label>
                </div>
              ) : null}

              {!draft.hasSecondHint ? (
                <div className="stack">
                  <label className="field">
                    <span>Вторая подсказка</span>
                    <textarea
                      className="field__textarea field__textarea--small"
                      rows="3"
                      value={draft.hintTwoText}
                      onChange={(event) => handleChange(task.id, 'hintTwoText', event.target.value)}
                      disabled={!canManageContent}
                    />
                  </label>
                  <label className="field">
                    <span>Пауза после первой подсказки, мин</span>
                    <input
                      type="number"
                      min="0"
                      value={draft.hintTwoDelay}
                      onChange={(event) => handleChange(task.id, 'hintTwoDelay', event.target.value)}
                      disabled={!canManageContent}
                    />
                  </label>
                </div>
              ) : null}

              <div className="list-card__actions">
                <button className="button button--primary" type="button" onClick={() => handleSaveHints(task)} disabled={!canManageContent || isLoading || (draft.hasFirstHint && draft.hasSecondHint)}>
                  {isLoading ? 'Сохраняем...' : 'Сохранить подсказки'}
                </button>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
