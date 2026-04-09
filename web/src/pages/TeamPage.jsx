import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
import {
  useApproveJoinRequestMutation,
  useGetCurrentTeamQuery,
  useGetIncomingJoinRequestsQuery,
  useGetOutgoingJoinRequestsQuery,
  useLeaveTeamMutation,
  useRejectJoinRequestMutation,
  useRemoveMemberMutation,
  useTransferCaptainRoleMutation,
} from '../features/team/teamApi.js'
import {
  formatDateTime,
  formatMembershipStatus,
  formatTeamRole,
} from '../shared/lib/formatters.js'

export function TeamPage() {
  const currentUser = useSelector((state) => state.auth.user)
  const [actionMessage, setActionMessage] = useState('')
  const [actionError, setActionError] = useState('')
  const [activeActionKey, setActiveActionKey] = useState('')
  const {
    data: team,
    isLoading: isTeamLoading,
    error: teamError,
  } = useGetCurrentTeamQuery()
  const { data: incomingRequests = [] } = useGetIncomingJoinRequestsQuery(undefined, {
    skip: !team,
  })
  const { data: outgoingRequests = [] } = useGetOutgoingJoinRequestsQuery()
  const [leaveTeam, { isLoading: isLeaving }] = useLeaveTeamMutation()
  const [approveJoinRequest] = useApproveJoinRequestMutation()
  const [rejectJoinRequest] = useRejectJoinRequestMutation()
  const [removeMember] = useRemoveMemberMutation()
  const [transferCaptainRole] = useTransferCaptainRoleMutation()

  const isCaptain = useMemo(() => {
    if (!team || !currentUser?.id) {
      return false
    }

    return team.captainId === currentUser.id
  }, [currentUser?.id, team])

  const handleLeaveTeam = async () => {
    setActionMessage('')
    setActionError('')

    try {
      const response = await leaveTeam().unwrap()
      setActionMessage(response.message || 'Вы вышли из команды')
    } catch (requestError) {
      setActionError(requestError?.message || 'Не удалось выйти из команды')
    }
  }

  const runCaptainAction = async (actionKey, action, successMessage) => {
    setActiveActionKey(actionKey)
    setActionMessage('')
    setActionError('')

    try {
      await action()
      setActionMessage(successMessage)
    } catch (requestError) {
      setActionError(requestError?.message || 'Не удалось выполнить действие')
    } finally {
      setActiveActionKey('')
    }
  }

  const error = teamError?.status && teamError.status !== 404 ? teamError.message : ''
  const status = isTeamLoading ? 'loading' : 'succeeded'

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Команда</p>
        <h1>Управление командой</h1>
        <p className="page-card__text">
          Здесь можно посмотреть состав команды, код приглашения и текущие заявки
          на вступление.
        </p>
      </div>

      {status === 'loading' ? <p className="page-note">Загрузка команды...</p> : null}
      {error ? <p className="form-message form-message--error">{error}</p> : null}
      {actionError ? <p className="form-message form-message--error">{actionError}</p> : null}
      {actionMessage ? <p className="form-message form-message--success">{actionMessage}</p> : null}

      {!team && status === 'succeeded' ? (
        <>
          <section className="empty-state">
            <h2>У тебя пока нет команды</h2>
            <p>
              Создай новую команду или присоединись к существующей по коду
              приглашения либо через список команд.
            </p>

            <div className="cta-group">
              <Link className="button button--primary" to="/teams/create">
                Создать команду
              </Link>
              <Link className="button button--secondary" to="/teams/join">
                Найти команду
              </Link>
            </div>
          </section>

          {outgoingRequests.length > 0 ? (
            <section className="section-block">
              <h2>Мои заявки на вступление</h2>
              <div className="list-grid">
                {outgoingRequests.map((request) => (
                  <article key={request.teamId} className="list-card">
                    <h3>{request.teamName}</h3>
                    <p>{request.city}</p>
                    <p>Статус: {formatMembershipStatus(request.status)}</p>
                    <p>Отправлена: {formatDateTime(request.createdAt)}</p>
                  </article>
                ))}
              </div>
            </section>
          ) : null}
        </>
      ) : null}

      {team ? (
        <div className="stack">
          <section className="section-block">
            <div className="section-block__header">
              <div>
                <h2>{team.name}</h2>
                <p className="section-block__text">Город: {team.city}</p>
              </div>
              {!isCaptain ? (
                <button
                  className="button button--secondary"
                  type="button"
                  onClick={handleLeaveTeam}
                  disabled={isLeaving}
                >
                  {isLeaving ? 'Выходим...' : 'Покинуть команду'}
                </button>
              ) : null}
            </div>

            <dl className="detail-grid">
              <div>
                <dt>Капитан</dt>
                <dd>{team.captainEmail}</dd>
              </div>
              <div>
                <dt>Код приглашения</dt>
                <dd>{team.inviteCode}</dd>
              </div>
              <div>
                <dt>Дата создания</dt>
                <dd>{formatDateTime(team.createdAt)}</dd>
              </div>
              <div>
                <dt>Роль в команде</dt>
                <dd>{isCaptain ? 'Капитан' : 'Участник'}</dd>
              </div>
            </dl>
          </section>

          <section className="section-block">
            <h2>Участники команды</h2>
            <div className="list-grid">
              {team.members.map((member) => (
                <article key={member.userId} className="list-card">
                  <h3>{member.email}</h3>
                  <p>Роль: {formatTeamRole(member.role)}</p>
                  <p>Статус: {formatMembershipStatus(member.status)}</p>
                  <p>Вступил: {formatDateTime(member.joinedAt)}</p>
                  {isCaptain && member.userId !== currentUser?.id ? (
                    <div className="list-card__actions">
                      <button
                        className="button button--secondary"
                        type="button"
                        onClick={() =>
                          runCaptainAction(
                            `remove-${member.userId}`,
                            () => removeMember({ teamId: team.id, userId: member.userId }).unwrap(),
                            'Участник исключён из команды',
                          )
                        }
                        disabled={activeActionKey === `remove-${member.userId}`}
                      >
                        {activeActionKey === `remove-${member.userId}`
                          ? 'Исключаем...'
                          : 'Исключить'}
                      </button>
                      <button
                        className="button button--secondary"
                        type="button"
                        onClick={() =>
                          runCaptainAction(
                            `transfer-${member.userId}`,
                            () =>
                              transferCaptainRole({ teamId: team.id, userId: member.userId }).unwrap(),
                            'Капитанство передано',
                          )
                        }
                        disabled={activeActionKey === `transfer-${member.userId}`}
                      >
                        {activeActionKey === `transfer-${member.userId}`
                          ? 'Передаём...'
                          : 'Сделать капитаном'}
                      </button>
                    </div>
                  ) : null}
                </article>
              ))}
            </div>
          </section>

          {isCaptain && incomingRequests.length > 0 ? (
            <section className="section-block">
              <h2>Входящие заявки</h2>
              <div className="list-grid">
                {incomingRequests.map((request) => (
                  <article key={request.userId} className="list-card">
                    <h3>{request.userEmail}</h3>
                    <p>Статус: {formatMembershipStatus(request.status)}</p>
                    <p>Отправлена: {formatDateTime(request.createdAt)}</p>
                    <div className="list-card__actions">
                      <button
                        className="button button--primary"
                        type="button"
                        onClick={() =>
                          runCaptainAction(
                            `approve-${request.userId}`,
                            () =>
                              approveJoinRequest({ teamId: team.id, userId: request.userId }).unwrap(),
                            'Заявка подтверждена',
                          )
                        }
                        disabled={activeActionKey === `approve-${request.userId}`}
                      >
                        {activeActionKey === `approve-${request.userId}`
                          ? 'Подтверждаем...'
                          : 'Подтвердить'}
                      </button>
                      <button
                        className="button button--secondary"
                        type="button"
                        onClick={() =>
                          runCaptainAction(
                            `reject-${request.userId}`,
                            () =>
                              rejectJoinRequest({ teamId: team.id, userId: request.userId }).unwrap(),
                            'Заявка отклонена',
                          )
                        }
                        disabled={activeActionKey === `reject-${request.userId}`}
                      >
                        {activeActionKey === `reject-${request.userId}`
                          ? 'Отклоняем...'
                          : 'Отклонить'}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}
