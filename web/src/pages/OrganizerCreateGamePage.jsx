import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCreateGameMutation } from '../features/game/gameApi.js'

const initialFormState = {
  title: '',
  description: '',
  city: '',
  minTeamSize: '2',
  maxTeamSize: '5',
  routeSlotsCount: '5',
  taskFailurePenaltyMinutes: '10',
  registrationStartsAt: '',
  registrationEndsAt: '',
  startsAt: '',
}

function toIsoOrNull(value) {
  return value ? new Date(value).toISOString() : null
}

export function OrganizerCreateGamePage() {
  const navigate = useNavigate()
  const [createGame, { isLoading }] = useCreateGameMutation()
  const [formData, setFormData] = useState(initialFormState)
  const [error, setError] = useState('')

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!formData.title.trim() || !formData.description.trim() || !formData.city.trim()) {
      setError('Заполни название, описание и город игры')
      return
    }

    if (!formData.startsAt) {
      setError('Укажи дату и время старта игры')
      return
    }

    if (Number(formData.minTeamSize) > Number(formData.maxTeamSize)) {
      setError('Минимальный размер команды не может быть больше максимального')
      return
    }

    if (Number(formData.routeSlotsCount) < 1) {
      setError('Количество маршрутов должно быть не меньше 1')
      return
    }

    setError('')

    try {
      const response = await createGame({
        title: formData.title.trim(),
        description: formData.description.trim(),
        city: formData.city.trim(),
        minTeamSize: Number(formData.minTeamSize),
        maxTeamSize: Number(formData.maxTeamSize),
        routeSlotsCount: Number(formData.routeSlotsCount),
        taskFailurePenaltyMinutes: Number(formData.taskFailurePenaltyMinutes),
        registrationStartsAt: toIsoOrNull(formData.registrationStartsAt),
        registrationEndsAt: toIsoOrNull(formData.registrationEndsAt),
        startsAt: new Date(formData.startsAt).toISOString(),
      }).unwrap()

      navigate(`/organizer/games/${response.id}`, { replace: true })
    } catch (requestError) {
      setError(requestError?.message || 'Не удалось создать игру')
    }
  }

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Организатор</p>
        <h1>Создание игры</h1>
        <p className="page-card__text">
          Заполни основные параметры игры. Позже можно будет добавить задания,
          маршруты и работу с заявками команд.
        </p>
      </div>

      <form className="auth-form" onSubmit={handleSubmit}>
        <div className="split-grid">
          <label className="field">
            <span>Название игры</span>
            <input
              name="title"
              type="text"
              placeholder="Например, Ночной дозор: Центр"
              value={formData.title}
              onChange={handleChange}
            />
          </label>

          <label className="field">
            <span>Город</span>
            <input
              name="city"
              type="text"
              placeholder="Например, Москва"
              value={formData.city}
              onChange={handleChange}
            />
          </label>
        </div>

        <label className="field">
          <span>Описание игры</span>
          <textarea
            className="field__textarea"
            name="description"
            placeholder="Кратко опиши идею игры, формат и условия участия"
            value={formData.description}
            onChange={handleChange}
            rows="5"
          />
        </label>

        <div className="split-grid split-grid--triple">
          <label className="field">
            <span>Мин. размер команды</span>
            <input
              name="minTeamSize"
              type="number"
              min="1"
              value={formData.minTeamSize}
              onChange={handleChange}
            />
          </label>

          <label className="field">
            <span>Макс. размер команды</span>
            <input
              name="maxTeamSize"
              type="number"
              min="1"
              value={formData.maxTeamSize}
              onChange={handleChange}
            />
          </label>

          <label className="field">
            <span>Количество маршрутов</span>
            <input
              name="routeSlotsCount"
              type="number"
              min="1"
              value={formData.routeSlotsCount}
              onChange={handleChange}
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
              value={formData.taskFailurePenaltyMinutes}
              onChange={handleChange}
            />
          </label>
        </div>

        <div className="split-grid">
          <label className="field">
            <span>Начало регистрации</span>
            <input
              name="registrationStartsAt"
              type="datetime-local"
              value={formData.registrationStartsAt}
              onChange={handleChange}
            />
          </label>

          <label className="field">
            <span>Конец регистрации</span>
            <input
              name="registrationEndsAt"
              type="datetime-local"
              value={formData.registrationEndsAt}
              onChange={handleChange}
            />
          </label>
        </div>

        <label className="field">
          <span>Старт игры</span>
          <input
            name="startsAt"
            type="datetime-local"
            value={formData.startsAt}
            onChange={handleChange}
          />
        </label>

        {error ? <p className="form-message form-message--error">{error}</p> : null}

        <button className="button button--primary" type="submit" disabled={isLoading}>
          {isLoading ? 'Создаём игру...' : 'Создать игру'}
        </button>
      </form>
    </section>
  )
}
