import { useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useSelector } from 'react-redux'
import {
  useCancelGameRegistrationMutation,
  useGetGamesQuery,
  useGetMyTeamRegistrationsQuery,
  useSubmitGameRegistrationMutation,
} from '../features/game/gameApi.js'
import { useGetCurrentTeamQuery } from '../features/team/teamApi.js'
import {
  formatDateTime,
  formatGameStatus,
  formatRegistrationStatus,
} from '../shared/lib/formatters.js'

export function GameDetailsPage() {
  const { gameId } = useParams()
  const navigate = useNavigate()
  const currentUser = useSelector((state) => state.auth.user)
  const { data: games = [], isFetching: isGamesLoading, error: gamesError } = useGetGamesQuery('')
  const { data: registrations = [] } = useGetMyTeamRegistrationsQuery()
  const { data: team } = useGetCurrentTeamQuery()
  const [submitGameRegistration, { isLoading: isSubmittingRegistration }] =
    useSubmitGameRegistrationMutation()
  const [cancelGameRegistration, { isLoading: isCancellingRegistration }] =
    useCancelGameRegistrationMutation()
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const game = useMemo(
    () => games.find((item) => String(item.id) === String(gameId)) || null,
    [gameId, games],
  )

  const registration = useMemo(
    () => registrations.find((item) => String(item.gameId) === String(gameId)),
    [gameId, registrations],
  )

  const isCaptain = Boolean(team && currentUser?.id && team.captainId === currentUser.id)
  const canSubmitRegistration =
    Boolean(team) &&
    isCaptain &&
    !registration &&
    game &&
    team.members.length >= game.minTeamSize &&
    team.members.length <= game.maxTeamSize
  const actionStatus =
    isSubmittingRegistration || isCancellingRegistration ? 'loading' : 'idle'
  const status = isGamesLoading ? 'loading' : 'succeeded'
  const pageError =
    error || gamesError?.message || (!isGamesLoading && !game ? 'Игра не найдена в публичном списке' : '')
  const isTeamSizeTooSmall = Boolean(team && game && team.members.length < game.minTeamSize)
  const isTeamSizeTooLarge = Boolean(team && game && team.members.length > game.maxTeamSize)
  const teamSizeFits = Boolean(team && game && !isTeamSizeTooSmall && !isTeamSizeTooLarge)

  const participationHint = (() => {
    if (registration) {
      const statusMap = {
        PENDING: 'Заявка уже отправлена и сейчас ожидает решения организатора.',
        APPROVED: 'Команда подтверждена и допущена к участию в игре.',
        REJECTED: 'Организатор отклонил заявку команды на участие.',
        CANCELED: 'Заявка на участие была отменена капитаном.',
      }

      return statusMap[registration.registrationStatus] || 'Статус заявки обновлён.'
    }

    if (!team) {
      return 'Участие станет доступно после создания команды или вступления в неё.'
    }

    if (!isCaptain) {
      return 'Отправлять заявку на участие может только капитан команды.'
    }

    if (isTeamSizeTooSmall) {
      return `Сейчас в команде ${team.members.length} участников, а нужно минимум ${game.minTeamSize}.`
    }

    if (isTeamSizeTooLarge) {
      return `Сейчас в команде ${team.members.length} участников, а допустимо максимум ${game.maxTeamSize}.`
    }

    return 'Команда подходит под условия игры и может подать заявку.'
  })()

  const handleSubmitRegistration = async () => {
    setError('')
    setMessage('')

    try {
      await submitGameRegistration(gameId).unwrap()
      setMessage('Заявка на участие успешно отправлена')
    } catch (requestError) {
      setError(requestError?.message || 'Не удалось отправить заявку')
    }
  }

  const handleCancelRegistration = async () => {
    setError('')
    setMessage('')

    try {
      await cancelGameRegistration(gameId).unwrap()
      setMessage('Заявка отменена')
    } catch (requestError) {
      setError(requestError?.message || 'Не удалось отменить заявку')
    }
  }

  return (
    <section className="page-card">
      <div className="page-card__header">
        <p className="page-card__eyebrow">Игра</p>
        <h1>{game?.title || 'Карточка игры'}</h1>
        <p className="page-card__text">
          Здесь собрана основная информация об игре и статус участия твоей команды.
        </p>
      </div>

      {status === 'loading' ? <p className="page-note">Загрузка информации об игре...</p> : null}
      {pageError ? <p className="form-message form-message--error">{pageError}</p> : null}
      {message ? <p className="form-message form-message--success">{message}</p> : null}

      {game ? (
        <div className="stack">
          <section className="section-block">
            <h2>Основная информация</h2>

            <dl className="detail-grid">
              <div>
                <dt>Город</dt>
                <dd>{game.city}</dd>
              </div>
              <div>
                <dt>Статус игры</dt>
                <dd>{formatGameStatus(game.status)}</dd>
              </div>
              <div>
                <dt>Размер команды</dt>
                <dd>
                  {game.minTeamSize}-{game.maxTeamSize} участников
                </dd>
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
                <dt>Начало регистрации</dt>
                <dd>{formatDateTime(game.registrationStartsAt)}</dd>
              </div>
              <div>
                <dt>Конец регистрации</dt>
                <dd>{formatDateTime(game.registrationEndsAt)}</dd>
              </div>
            </dl>
          </section>

          <section className="section-block">
            <div className="section-block__header">
              <div>
                <h2>Участие команды</h2>
                <p className="section-block__text">
                  Команда: {team ? team.name : 'Команда пока не создана'}
                </p>
              </div>
              <button
                className="button button--secondary"
                type="button"
                onClick={() => navigate('/games')}
              >
                Ко всем играм
              </button>
            </div>

            {team ? (
              <dl className="detail-grid">
                <div>
                  <dt>Размер текущей команды</dt>
                  <dd>{team.members.length} участников</dd>
                </div>
                <div>
                  <dt>Капитан</dt>
                  <dd>{team.captainEmail}</dd>
                </div>
                <div>
                  <dt>Статус заявки</dt>
                  <dd>{registration ? formatRegistrationStatus(registration.registrationStatus) : 'Не подана'}</dd>
                </div>
                <div>
                  <dt>Право подачи заявки</dt>
                  <dd>{isCaptain ? 'Да, ты капитан' : 'Нет, только капитан'}</dd>
                </div>
                <div>
                  <dt>Соответствие по размеру</dt>
                  <dd>{teamSizeFits ? 'Команда подходит' : 'Есть ограничения'}</dd>
                </div>
              </dl>
            ) : (
              <div className="cta-group">
                <p className="page-note">
                  Чтобы подать заявку на игру, сначала создай команду или вступи в существующую.
                </p>
                <Link className="button button--secondary" to="/teams/create">
                  Создать команду
                </Link>
                <Link className="button button--secondary" to="/teams/join">
                  Найти команду
                </Link>
              </div>
            )}

            <div className="cta-group">
              <span className="badge">{participationHint}</span>

              {canSubmitRegistration ? (
                <button
                  className="button button--primary"
                  type="button"
                  onClick={handleSubmitRegistration}
                  disabled={actionStatus === 'loading'}
                >
                  {actionStatus === 'loading' ? 'Отправляем...' : 'Подать заявку на участие'}
                </button>
              ) : null}

              {registration?.registrationStatus === 'PENDING' ? (
                <button
                  className="button button--secondary"
                  type="button"
                  onClick={handleCancelRegistration}
                  disabled={actionStatus === 'loading'}
                >
                  {actionStatus === 'loading' ? 'Отменяем...' : 'Отменить заявку'}
                </button>
              ) : null}

              {team && !isCaptain ? <span className="badge">Заявку подаёт только капитан</span> : null}
              {team && isCaptain && isTeamSizeTooSmall ? (
                <span className="badge badge--danger">Недостаточно участников</span>
              ) : null}
              {team && isCaptain && isTeamSizeTooLarge ? (
                <span className="badge badge--danger">Команда превышает лимит</span>
              ) : null}
              {registration?.registrationStatus === 'APPROVED' ? (
                <span className="badge badge--success">Заявка подтверждена</span>
              ) : null}
              {registration?.registrationStatus === 'REJECTED' ? (
                <span className="badge badge--danger">Заявка отклонена</span>
              ) : null}
              {registration?.registrationStatus === 'CANCELED' ? (
                <span className="badge">Заявка отменена</span>
              ) : null}
              {team && isCaptain && !registration && !canSubmitRegistration && !isTeamSizeTooSmall && !isTeamSizeTooLarge ? (
                <span className="badge">
                  Сейчас заявка недоступна
                </span>
              ) : null}
            </div>
          </section>
        </div>
      ) : null}
    </section>
  )
}
