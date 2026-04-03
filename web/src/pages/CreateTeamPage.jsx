import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCreateTeamMutation } from '../features/team/teamApi.js'

const initialFormState = {
  name: '',
  city: '',
}

export function CreateTeamPage() {
  const navigate = useNavigate()
  const [createTeam, { isLoading: isSubmitting }] = useCreateTeamMutation()
  const [formData, setFormData] = useState(initialFormState)
  const [error, setError] = useState('')

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!formData.name.trim() || !formData.city.trim()) {
      setError('Заполни название команды и город')
      return
    }

    setError('')

    try {
      await createTeam({
        name: formData.name.trim(),
        city: formData.city.trim(),
      }).unwrap()
      navigate('/team', { replace: true })
    } catch (requestError) {
      setError(requestError?.message || 'Не удалось создать команду')
    }
  }

  return (
    <section className="page-card page-card--narrow">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Команда</p>
        <h1>Создание команды</h1>
        <p className="page-card__text">
          Задай название и город команды. После создания ты станешь капитаном.
        </p>
      </div>

      <form className="auth-form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Название команды</span>
          <input
            name="name"
            type="text"
            placeholder="Например, Ночной маршрут"
            value={formData.name}
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

        {error ? <p className="form-message form-message--error">{error}</p> : null}

        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Создаём...' : 'Создать команду'}
        </button>
      </form>
    </section>
  )
}
