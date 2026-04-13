import { useMemo, useState } from 'react'
import { useOutletContext, useParams } from 'react-router-dom'
import {
  useCreateTaskMutation,
  useDeleteTaskMutation,
  useGetOrganizerGameRoutesQuery,
  useGetOrganizerGameTasksQuery,
  useUpdateTaskMutation,
} from '../features/game/gameApi.js'

const initialTaskForm = {
  title: '',
  riddleText: '',
  answerKey: '',
  orderIndex: '1',
  timeLimitMinutes: '60',
  failurePenaltyMinutes: '10',
}

export function OrganizerGameTasksPage() {
  const { gameId } = useParams()
  const { canManageContent } = useOutletContext()
  const { data: tasks = [], isFetching, error } = useGetOrganizerGameTasksQuery(gameId)
  const { data: routes = [] } = useGetOrganizerGameRoutesQuery(gameId)
  const [createTask, { isLoading: isCreatingTask }] = useCreateTaskMutation()
  const [updateTask, { isLoading: isUpdatingTask }] = useUpdateTaskMutation()
  const [deleteTask, { isLoading: isDeletingTask }] = useDeleteTaskMutation()
  const [taskForm, setTaskForm] = useState(initialTaskForm)
  const [editingTaskId, setEditingTaskId] = useState(null)
  const [taskEditForm, setTaskEditForm] = useState(initialTaskForm)
  const [taskMessage, setTaskMessage] = useState('')
  const [taskError, setTaskError] = useState('')

  const assignedTaskIds = useMemo(
    () => new Set(routes.flatMap((route) => route.items.map((item) => String(item.taskId)))),
    [routes],
  )

  const handleTaskFormChange = (event) => {
    const { name, value } = event.target
    setTaskForm((current) => ({ ...current, [name]: value }))
  }

  const handleTaskEditFormChange = (event) => {
    const { name, value } = event.target
    setTaskEditForm((current) => ({ ...current, [name]: value }))
  }

  const startTaskEditing = (task) => {
    setEditingTaskId(task.id)
    setTaskEditForm({
      title: task.title,
      riddleText: task.riddleText,
      answerKey: task.answerKey,
      orderIndex: String(task.orderIndex),
      timeLimitMinutes: String(task.timeLimitMinutes),
      failurePenaltyMinutes: String(task.failurePenaltyMinutes),
    })
  }

  const cancelTaskEditing = () => {
    setEditingTaskId(null)
    setTaskEditForm(initialTaskForm)
  }

  const handleCreateTask = async (event) => {
    event.preventDefault()
    setTaskError('')
    setTaskMessage('')

    try {
      await createTask({
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

      setTaskMessage('Задание создано')
      setTaskForm({
        ...initialTaskForm,
        orderIndex: String(tasks.length + 2),
      })
    } catch (requestError) {
      setTaskError(requestError?.message || 'Не удалось создать задание')
    }
  }

  const handleUpdateTask = async (taskId) => {
    setTaskError('')
    setTaskMessage('')

    try {
      await updateTask({
        gameId,
        taskId,
        payload: {
          title: taskEditForm.title.trim(),
          riddleText: taskEditForm.riddleText.trim(),
          answerKey: taskEditForm.answerKey.trim(),
          orderIndex: Number(taskEditForm.orderIndex),
          timeLimitMinutes: Number(taskEditForm.timeLimitMinutes),
          failurePenaltyMinutes: Number(taskEditForm.failurePenaltyMinutes),
        },
      }).unwrap()

      setTaskMessage('Задание обновлено')
      cancelTaskEditing()
    } catch (requestError) {
      setTaskError(requestError?.message || 'Не удалось обновить задание')
    }
  }

  const handleDeleteTask = async (taskId) => {
    setTaskError('')
    setTaskMessage('')

    try {
      await deleteTask({ gameId, taskId }).unwrap()
      setTaskMessage('Задание удалено')
      if (editingTaskId === taskId) {
        cancelTaskEditing()
      }
    } catch (requestError) {
      setTaskError(requestError?.message || 'Не удалось удалить задание')
    }
  }

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Экран управления заданиями</h2>
          <p className="section-block__text">
            Создавай загадки, редактируй их параметры и убирай лишние задания до старта игры.
          </p>
        </div>
      </div>

      {error?.message ? <p className="form-message form-message--error">{error.message}</p> : null}
      {taskError ? <p className="form-message form-message--error">{taskError}</p> : null}
      {taskMessage ? <p className="form-message form-message--success">{taskMessage}</p> : null}
      {!canManageContent ? (
        <p className="form-message form-message--error">
          После старта или отмены игры задания больше нельзя менять.
        </p>
      ) : null}

      <form className="auth-form" onSubmit={handleCreateTask}>
        <div className="split-grid">
          <label className="field">
            <span>Название</span>
            <input name="title" value={taskForm.title} onChange={handleTaskFormChange} disabled={!canManageContent} />
          </label>
          <label className="field">
            <span>Ключ</span>
            <input name="answerKey" value={taskForm.answerKey} onChange={handleTaskFormChange} disabled={!canManageContent} />
          </label>
        </div>

        <label className="field">
          <span>Текст загадки</span>
          <textarea
            className="field__textarea"
            name="riddleText"
            rows="5"
            value={taskForm.riddleText}
            onChange={handleTaskFormChange}
            disabled={!canManageContent}
          />
        </label>

        <div className="split-grid split-grid--triple">
          <label className="field">
            <span>Порядок</span>
            <input name="orderIndex" type="number" min="1" value={taskForm.orderIndex} onChange={handleTaskFormChange} disabled={!canManageContent} />
          </label>
          <label className="field">
            <span>Лимит времени, мин</span>
            <input name="timeLimitMinutes" type="number" min="1" value={taskForm.timeLimitMinutes} onChange={handleTaskFormChange} disabled={!canManageContent} />
          </label>
          <label className="field">
            <span>Штраф, мин</span>
            <input name="failurePenaltyMinutes" type="number" min="0" value={taskForm.failurePenaltyMinutes} onChange={handleTaskFormChange} disabled={!canManageContent} />
          </label>
        </div>

        <button className="button button--primary" type="submit" disabled={!canManageContent || isCreatingTask}>
          {isCreatingTask ? 'Сохраняем...' : 'Создать задание'}
        </button>
      </form>

      {isFetching ? <p className="page-note">Загрузка заданий...</p> : null}

      {tasks.length === 0 && !isFetching ? (
        <section className="empty-state">
          <h2>Заданий пока нет</h2>
          <p>Создай первую загадку, чтобы затем добавить к ней подсказки и распределить по маршрутам.</p>
        </section>
      ) : null}

      <div className="list-grid list-grid--balanced">
        {tasks.map((task) => {
          const isEditing = editingTaskId === task.id

          return (
            <article key={task.id} className="list-card">
              {isEditing ? (
                <>
                  <div className="split-grid">
                    <label className="field">
                      <span>Название</span>
                      <input name="title" value={taskEditForm.title} onChange={handleTaskEditFormChange} />
                    </label>
                    <label className="field">
                      <span>Ключ</span>
                      <input name="answerKey" value={taskEditForm.answerKey} onChange={handleTaskEditFormChange} />
                    </label>
                  </div>
                  <label className="field">
                    <span>Текст загадки</span>
                    <textarea className="field__textarea field__textarea--small" name="riddleText" rows="4" value={taskEditForm.riddleText} onChange={handleTaskEditFormChange} />
                  </label>
                  <div className="split-grid split-grid--triple">
                    <label className="field">
                      <span>Порядок</span>
                      <input name="orderIndex" type="number" min="1" value={taskEditForm.orderIndex} onChange={handleTaskEditFormChange} />
                    </label>
                    <label className="field">
                      <span>Лимит, мин</span>
                      <input name="timeLimitMinutes" type="number" min="1" value={taskEditForm.timeLimitMinutes} onChange={handleTaskEditFormChange} />
                    </label>
                    <label className="field">
                      <span>Штраф, мин</span>
                      <input name="failurePenaltyMinutes" type="number" min="0" value={taskEditForm.failurePenaltyMinutes} onChange={handleTaskEditFormChange} />
                    </label>
                  </div>
                  <div className="list-card__actions">
                    <button className="button button--primary" type="button" onClick={() => handleUpdateTask(task.id)} disabled={isUpdatingTask}>
                      Сохранить
                    </button>
                    <button className="button button--secondary" type="button" onClick={cancelTaskEditing}>
                      Отмена
                    </button>
                  </div>
                </>
              ) : (
                <>
                  <h3>{task.title}</h3>
                  <p>Порядок: {task.orderIndex}</p>
                  <p>Лимит времени: {task.timeLimitMinutes} мин</p>
                  <p>Штраф: {task.failurePenaltyMinutes} мин</p>
                  <p>Подсказок: {task.hints.length} из 2</p>
                  <p>{assignedTaskIds.has(String(task.id)) ? 'Задание уже включено в маршрут' : 'Задание ещё не используется в маршрутах'}</p>
                  <div className="list-card__actions">
                    <button className="button button--secondary" type="button" onClick={() => startTaskEditing(task)} disabled={!canManageContent}>
                      Редактировать
                    </button>
                    <button className="button button--secondary" type="button" onClick={() => handleDeleteTask(task.id)} disabled={!canManageContent || isDeletingTask}>
                      Удалить
                    </button>
                  </div>
                </>
              )}
            </article>
          )
        })}
      </div>
    </section>
  )
}
