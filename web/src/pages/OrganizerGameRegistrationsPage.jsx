import { useOutletContext, useParams } from 'react-router-dom'
import {
  useApproveRegistrationMutation,
  useGetIncomingRegistrationsQuery,
  useRejectRegistrationMutation,
} from '../features/game/gameApi.js'
import { formatRegistrationStatus } from '../shared/lib/formatters.js'
import { useState } from 'react'

export function OrganizerGameRegistrationsPage() {
  const { gameId } = useParams()
  const { canReviewRegistrations } = useOutletContext()
  const { data: incomingRegistrations = [], isFetching, error } = useGetIncomingRegistrationsQuery(gameId)
  const [approveRegistration, { isLoading: isApproving }] = useApproveRegistrationMutation()
  const [rejectRegistration, { isLoading: isRejecting }] = useRejectRegistrationMutation()
  const [message, setMessage] = useState('')
  const [requestError, setRequestError] = useState('')

  const handleRegistrationDecision = async (registrationId, decision) => {
    setRequestError('')
    setMessage('')

    try {
      if (decision === 'approve') {
        await approveRegistration({ gameId, registrationId }).unwrap()
        setMessage('Заявка команды подтверждена, маршрут назначен автоматически')
      } else {
        await rejectRegistration({ gameId, registrationId }).unwrap()
        setMessage('Заявка команды отклонена')
      }
    } catch (decisionError) {
      setRequestError(decisionError?.message || 'Не удалось обработать заявку')
    }
  }

  return (
    <section className="section-block">
      <div className="section-block__header">
        <div>
          <h2>Экран просмотра заявок команд</h2>
          <p className="section-block__text">
            Здесь можно увидеть все входящие заявки и решить, какие команды будут участвовать в игре.
          </p>
        </div>
      </div>

      {error?.message ? <p className="form-message form-message--error">{error.message}</p> : null}
      {requestError ? <p className="form-message form-message--error">{requestError}</p> : null}
      {message ? <p className="form-message form-message--success">{message}</p> : null}
      {!canReviewRegistrations ? (
        <p className="form-message form-message--error">
          После старта или отмены игры заявки больше нельзя обрабатывать.
        </p>
      ) : null}

      {isFetching ? <p className="page-note">Загрузка заявок команд...</p> : null}

      {incomingRegistrations.length === 0 && !isFetching ? (
        <section className="empty-state">
          <h2>Заявок пока нет</h2>
          <p>Когда капитаны команд отправят заявки на игру, они появятся здесь.</p>
        </section>
      ) : null}

      <div className="list-grid list-grid--balanced">
        {incomingRegistrations.map((registration) => (
          <article key={registration.id} className="list-card">
            <h3>{registration.teamName}</h3>
            <p>Капитан: {registration.captainEmail}</p>
            <p>Участников: {registration.memberCount}</p>
            <p>Статус: {formatRegistrationStatus(registration.status)}</p>
            <div className="list-card__actions">
              <button className="button button--primary" type="button" onClick={() => handleRegistrationDecision(registration.id, 'approve')} disabled={!canReviewRegistrations || registration.status !== 'PENDING' || isApproving || isRejecting}>
                Подтвердить
              </button>
              <button className="button button--secondary" type="button" onClick={() => handleRegistrationDecision(registration.id, 'reject')} disabled={!canReviewRegistrations || registration.status !== 'PENDING' || isApproving || isRejecting}>
                Отклонить
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
