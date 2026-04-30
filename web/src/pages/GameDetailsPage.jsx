import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { useSelector } from 'react-redux'
import {
  useCancelGameRegistrationMutation,
  useGetCaptainOrganizerChatMessagesForCaptainQuery,
  useGetGamesQuery,
  useGetMyTeamRegistrationsQuery,
  useGetMyTeamProgressQuery,
  useSubmitGameRegistrationMutation,
} from '../features/game/gameApi.js'
import { useGetCurrentTeamQuery } from '../features/team/teamApi.js'
import {
  formatDateTime,
  formatGameStatus,
  formatRegistrationStatus,
} from '../shared/lib/formatters.js'
import { buildChatSocketUrl, parseSocketEvent } from '../shared/lib/chatSocket.js'

export function GameDetailsPage() {
  const { gameId } = useParams()
  const navigate = useNavigate()
  const currentUser = useSelector((state) => state.auth.user)
  const token = useSelector((state) => state.auth.token)
  const { data: games = [], isFetching: isGamesLoading, error: gamesError } = useGetGamesQuery('')
  const { data: registrations = [] } = useGetMyTeamRegistrationsQuery()
  const { data: team } = useGetCurrentTeamQuery()
  const game = useMemo(
    () => games.find((item) => String(item.id) === String(gameId)) || null,
    [gameId, games],
  )
  const registration = useMemo(
    () => registrations.find((item) => String(item.gameId) === String(gameId)),
    [gameId, registrations],
  )
  const isFinishedGame = game?.status === 'FINISHED'
  const isCanceledGame = game?.status === 'CANCELED'
  const {
    data: teamProgress,
    isFetching: isTeamProgressLoading,
    error: teamProgressError,
  } = useGetMyTeamProgressQuery(undefined, {
    skip: !team || !isFinishedGame,
  })
  const [submitGameRegistration, { isLoading: isSubmittingRegistration }] =
    useSubmitGameRegistrationMutation()
  const [cancelGameRegistration, { isLoading: isCancellingRegistration }] =
    useCancelGameRegistrationMutation()
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [chatMessage, setChatMessage] = useState('')
  const [chatError, setChatError] = useState('')
  const [isSendingCaptainOrganizerMessage, setIsSendingCaptainOrganizerMessage] = useState(false)
  const [isAwaitingCaptainOrganizerAck, setIsAwaitingCaptainOrganizerAck] = useState(false)
  const [chatSocket, setChatSocket] = useState(null)
  const [chatFeed, setChatFeed] = useState([])
  const chatWindowRef = useRef(null)
  const chatInputRef = useRef(null)
  const isCaptain = Boolean(team && currentUser?.id && team.captainId === currentUser.id)
  const isApprovedRegistration = registration?.registrationStatus === 'APPROVED'
  const resultBelongsToCurrentGame =
    isFinishedGame && teamProgress && String(teamProgress.gameId) === String(gameId)
  const {
    data: captainOrganizerMessages = [],
    isFetching: isCaptainOrganizerChatLoading,
    error: captainOrganizerChatError,
  } = useGetCaptainOrganizerChatMessagesForCaptainQuery(gameId, {
    skip: !isCaptain || !isApprovedRegistration,
  })

  useEffect(() => {
    setChatFeed(captainOrganizerMessages)
  }, [captainOrganizerMessages])

  useEffect(() => {
    if (!chatWindowRef.current) {
      return
    }

    chatWindowRef.current.scrollTop = chatWindowRef.current.scrollHeight
  }, [chatFeed])

  const resizeChatInput = (target) => {
    if (!target) {
      return
    }

    target.style.height = 'auto'
    target.style.height = `${Math.min(target.scrollHeight, 180)}px`
  }

  useEffect(() => {
    if (!isCaptain || !isApprovedRegistration || !team?.id || !token) {
      return undefined
    }

    const socket = new WebSocket(buildChatSocketUrl(token))
    setChatSocket(socket)

    socket.onopen = () => {
      socket.send(
        JSON.stringify({
          type: 'SUBSCRIBE',
          gameId: Number(gameId),
          teamId: Number(team.id),
          channel: 'CAPTAIN_ORGANIZER',
        }),
      )
    }

    socket.onmessage = (rawEvent) => {
      const event = parseSocketEvent(rawEvent)
      if (!event) {
        return
      }

      if (event.type === 'ERROR') {
        setChatError(event.payload?.error || 'Сервер отклонил сообщение в чате')
        setIsAwaitingCaptainOrganizerAck(false)
        return
      }

      if (event.type !== 'MESSAGE') {
        return
      }

      const messagePayload = event.payload?.message
      if (!messagePayload) {
        return
      }

      setChatFeed((current) => {
        if (current.some((item) => item.id === messagePayload.id)) {
          return current
        }
        return [...current, messagePayload]
      })

      if (messagePayload.senderEmail === currentUser?.email) {
        setIsAwaitingCaptainOrganizerAck(false)
      }
    }

    socket.onerror = () => {
      setChatError('Ошибка соединения чата. Попробуй обновить страницу.')
      setIsAwaitingCaptainOrganizerAck(false)
    }

    socket.onclose = () => {
      setChatSocket(null)
    }

    return () => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(
          JSON.stringify({
            type: 'UNSUBSCRIBE',
            gameId: Number(gameId),
            teamId: Number(team.id),
            channel: 'CAPTAIN_ORGANIZER',
          }),
        )
      }
      socket.close()
    }
  }, [currentUser?.email, gameId, isApprovedRegistration, isCaptain, team?.id, token])
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

  const formatDuration = (totalSeconds) => {
    const hours = Math.floor(totalSeconds / 3600)
    const minutes = Math.floor((totalSeconds % 3600) / 60)
    const seconds = totalSeconds % 60

    return [hours, minutes, seconds]
      .map((value) => String(value).padStart(2, '0'))
      .join(':')
  }

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

  const handleSendCaptainOrganizerMessage = async () => {
    if (!chatMessage.trim()) {
      return
    }

    setChatError('')
    setIsSendingCaptainOrganizerMessage(true)

    try {
      if (!chatSocket || chatSocket.readyState !== WebSocket.OPEN) {
        throw new Error('Чат не подключен. Обнови страницу.')
      }

      chatSocket.send(
        JSON.stringify({
          type: 'SEND',
          gameId: Number(gameId),
          teamId: Number(team?.id),
          channel: 'CAPTAIN_ORGANIZER',
          text: chatMessage.trim(),
        }),
      )
      setIsAwaitingCaptainOrganizerAck(true)
      setChatMessage('')
      if (chatInputRef.current) {
        resizeChatInput(chatInputRef.current)
      }
    } catch (requestError) {
      setChatError(requestError?.message || 'Не удалось отправить сообщение')
      setIsAwaitingCaptainOrganizerAck(false)
    } finally {
      setIsSendingCaptainOrganizerMessage(false)
    }
  }

  const handleChatInputKeyDown = (event) => {
    if (event.key !== 'Enter') {
      return
    }

    if (event.ctrlKey) {
      return
    }

    event.preventDefault()
    void handleSendCaptainOrganizerMessage()
  }

  return (
    isCanceledGame ? (
      <Navigate to="/games" replace />
    ) : (
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

          {isFinishedGame ? (
            <section className="section-block">
              <div className="section-block__header">
                <div>
                  <h2>Результат игры</h2>
                  <p className="section-block__text">
                    Игра завершена. Для капитана доступен только итоговый результат команды и общий зачёт.
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

              {isTeamProgressLoading ? <p className="page-note">Загрузка результата команды...</p> : null}
              {teamProgressError?.message ? (
                <p className="form-message form-message--error">{teamProgressError.message}</p>
              ) : null}

              {!isTeamProgressLoading && !teamProgressError && !resultBelongsToCurrentGame ? (
                <section className="empty-state">
                  <h2>Результат недоступен</h2>
                  <p>
                    Для твоей команды нет итогового результата по этой игре. Возможно, команда не участвовала в ней или игровая сессия не была создана.
                  </p>
                </section>
              ) : null}

              {resultBelongsToCurrentGame ? (
                <div className="stack">
                  <dl className="detail-grid">
                    <div>
                      <dt>Место команды</dt>
                      <dd>{teamProgress.currentPlace}</dd>
                    </div>
                    <div>
                      <dt>Команда</dt>
                      <dd>{teamProgress.teamName}</dd>
                    </div>
                    <div>
                      <dt>Пройдено этапов</dt>
                      <dd>
                        {teamProgress.completedTasksCount} из {teamProgress.totalTasksCount}
                      </dd>
                    </div>
                    <div>
                      <dt>Штраф</dt>
                      <dd>{teamProgress.totalPenaltyMinutes} мин.</dd>
                    </div>
                    <div>
                      <dt>Время прохождения</dt>
                      <dd>{formatDuration(teamProgress.elapsedSeconds)}</dd>
                    </div>
                    <div>
                      <dt>Итоговый score</dt>
                      <dd>{formatDuration(teamProgress.totalScoreSeconds)}</dd>
                    </div>
                    <div>
                      <dt>Статус сессии</dt>
                      <dd>{teamProgress.sessionStatus}</dd>
                    </div>
                    <div>
                      <dt>Завершение</dt>
                      <dd>{teamProgress.finishedAt ? formatDateTime(teamProgress.finishedAt) : 'Не указано'}</dd>
                    </div>
                  </dl>

                  <div className="list-grid">
                    {teamProgress.standings.map((standing) => (
                      <article key={standing.teamId} className="list-card">
                        <h3>
                          #{standing.place} {standing.teamName}
                        </h3>
                        <p>
                          Пройдено этапов: {standing.completedTasksCount} из {standing.totalTasksCount}
                        </p>
                        <p>Штраф: {standing.totalPenaltyMinutes} мин.</p>
                        <p>Время прохождения: {formatDuration(standing.elapsedSeconds)}</p>
                        <p>Итоговый score: {formatDuration(standing.totalScoreSeconds)}</p>
                        <p>Статус сессии: {standing.sessionStatus}</p>
                        <p>
                          Завершение:{' '}
                          {standing.finishedAt ? formatDateTime(standing.finishedAt) : 'Команда не завершила игру'}
                        </p>
                      </article>
                    ))}
                  </div>
                </div>
              ) : null}
            </section>
          ) : null}

          {!isFinishedGame ? (
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
          ) : null}

          {isCaptain && !isFinishedGame ? (
            <section className="section-block">
              <div className="section-block__header">
                <div>
                  <h2>Чат капитана с организатором</h2>
                  <p className="section-block__text">
                    Этот канал доступен капитану после подтверждения участия команды в игре.
                  </p>
                </div>
              </div>

              {!isApprovedRegistration ? (
                <p className="page-note">
                  Чат станет доступен, когда заявка команды получит статус «Подтверждена».
                </p>
              ) : null}

              {isApprovedRegistration && isCaptainOrganizerChatLoading ? (
                <p className="page-note">Загрузка сообщений...</p>
              ) : null}
              {isApprovedRegistration && captainOrganizerChatError?.message ? (
                <p className="form-message form-message--error">{captainOrganizerChatError.message}</p>
              ) : null}
              {chatError ? <p className="form-message form-message--error">{chatError}</p> : null}

              {isApprovedRegistration ? (
                <div className="stack">
                  <div ref={chatWindowRef} className="chat-window">
                    {chatFeed.length === 0 ? (
                      <p className="page-note">Сообщений пока нет. Начни диалог первым.</p>
                    ) : (
                      chatFeed.map((chatItem) => (
                        <article
                          key={chatItem.id}
                          className={
                            chatItem.senderEmail === currentUser?.email
                              ? 'chat-message chat-message--own'
                              : 'chat-message'
                          }
                        >
                          <div className="chat-message__author">{chatItem.senderEmail}</div>
                          <div className="chat-message__text">{chatItem.text}</div>
                          <div className="chat-message__time">{formatDateTime(chatItem.createdAt)}</div>
                        </article>
                      ))
                    )}
                  </div>

                  <div className="chat-composer">
                    <textarea
                      ref={chatInputRef}
                      className="field__textarea field__textarea--small chat-composer__input"
                      rows="1"
                      value={chatMessage}
                      onChange={(event) => setChatMessage(event.target.value)}
                      onInput={(event) => resizeChatInput(event.target)}
                      onKeyDown={handleChatInputKeyDown}
                      placeholder="Напиши сообщение организатору"
                    />
                    <button
                      className="button button--primary"
                      type="button"
                      onClick={handleSendCaptainOrganizerMessage}
                      disabled={
                        isSendingCaptainOrganizerMessage ||
                        isAwaitingCaptainOrganizerAck ||
                        !chatMessage.trim()
                      }
                    >
                      {isSendingCaptainOrganizerMessage || isAwaitingCaptainOrganizerAck
                        ? 'Отправляем...'
                        : 'Отправить'}
                    </button>
                  </div>
                </div>
              ) : null}
            </section>
          ) : null}
        </div>
      ) : null}
    </section>
    )
  )
}
