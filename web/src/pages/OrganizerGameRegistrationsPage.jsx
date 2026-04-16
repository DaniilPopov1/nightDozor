import { useOutletContext, useParams } from 'react-router-dom'
import {
  useApproveRegistrationMutation,
  useGetCaptainOrganizerChatMessagesForOrganizerQuery,
  useGetIncomingRegistrationsQuery,
  useGetOrganizerGameRoutesQuery,
  useRejectRegistrationMutation,
} from '../features/game/gameApi.js'
import { formatDateTime, formatRegistrationStatus } from '../shared/lib/formatters.js'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useSelector } from 'react-redux'
import { buildChatSocketUrl, parseSocketEvent } from '../shared/lib/chatSocket.js'

export function OrganizerGameRegistrationsPage() {
  const { gameId } = useParams()
  const currentUser = useSelector((state) => state.auth.user)
  const token = useSelector((state) => state.auth.token)
  const { canReviewRegistrations } = useOutletContext()
  const { data: incomingRegistrations = [], isFetching, error } = useGetIncomingRegistrationsQuery(gameId)
  const { data: routes = [] } = useGetOrganizerGameRoutesQuery(gameId)
  const [approveRegistration, { isLoading: isApproving }] = useApproveRegistrationMutation()
  const [rejectRegistration, { isLoading: isRejecting }] = useRejectRegistrationMutation()
  const [isSendingMessage, setIsSendingMessage] = useState(false)
  const [message, setMessage] = useState('')
  const [requestError, setRequestError] = useState('')
  const [selectedTeamId, setSelectedTeamId] = useState('')
  const [chatText, setChatText] = useState('')
  const [chatError, setChatError] = useState('')
  const [chatSocket, setChatSocket] = useState(null)
  const [chatFeed, setChatFeed] = useState([])
  const [isAwaitingChatAck, setIsAwaitingChatAck] = useState(false)
  const chatWindowRef = useRef(null)
  const chatInputRef = useRef(null)

  const approvedTeams = useMemo(
    () =>
      routes
        .filter((route) => route.assignedTeamId && route.assignedTeamName)
        .map((route) => ({
          teamId: String(route.assignedTeamId),
          teamName: route.assignedTeamName,
        }))
        .filter((item, index, array) => array.findIndex((candidate) => candidate.teamId === item.teamId) === index),
    [routes],
  )

  const effectiveSelectedTeamId = useMemo(() => {
    if (approvedTeams.length === 0) {
      return ''
    }

    if (approvedTeams.some((team) => team.teamId === String(selectedTeamId))) {
      return selectedTeamId
    }

    return approvedTeams[0].teamId
  }, [approvedTeams, selectedTeamId])

  const {
    data: captainOrganizerMessages = [],
    isFetching: isCaptainOrganizerChatLoading,
    error: captainOrganizerChatError,
  } = useGetCaptainOrganizerChatMessagesForOrganizerQuery(
    { gameId, teamId: effectiveSelectedTeamId },
    { skip: !effectiveSelectedTeamId },
  )

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
    if (!effectiveSelectedTeamId || !token) {
      return undefined
    }

    const socket = new WebSocket(buildChatSocketUrl(token))
    setChatSocket(socket)

    socket.onopen = () => {
      socket.send(
        JSON.stringify({
          type: 'SUBSCRIBE',
          gameId: Number(gameId),
          teamId: Number(effectiveSelectedTeamId),
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
        setIsAwaitingChatAck(false)
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
        setIsAwaitingChatAck(false)
      }
    }

    socket.onerror = () => {
      setChatError('Ошибка соединения чата. Попробуй обновить страницу.')
      setIsAwaitingChatAck(false)
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
            teamId: Number(effectiveSelectedTeamId),
            channel: 'CAPTAIN_ORGANIZER',
          }),
        )
      }
      socket.close()
    }
  }, [currentUser?.email, effectiveSelectedTeamId, gameId, token])

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

  const handleSendMessage = async () => {
    if (!effectiveSelectedTeamId || !chatText.trim()) {
      return
    }

    setChatError('')
    setIsSendingMessage(true)

    try {
      if (!chatSocket || chatSocket.readyState !== WebSocket.OPEN) {
        throw new Error('Чат не подключен. Обнови страницу.')
      }

      chatSocket.send(
        JSON.stringify({
          type: 'SEND',
          gameId: Number(gameId),
          teamId: Number(effectiveSelectedTeamId),
          channel: 'CAPTAIN_ORGANIZER',
          text: chatText.trim(),
        }),
      )
      setIsAwaitingChatAck(true)
      setChatText('')
      if (chatInputRef.current) {
        resizeChatInput(chatInputRef.current)
      }
    } catch (sendError) {
      setChatError(sendError?.message || 'Не удалось отправить сообщение')
      setIsAwaitingChatAck(false)
    } finally {
      setIsSendingMessage(false)
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
    void handleSendMessage()
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

      <section className="section-block">
        <div className="section-block__header">
          <div>
            <h2>Чат капитан ↔ организатор</h2>
            <p className="section-block__text">
              Канал связи доступен только для подтверждённых команд.
            </p>
          </div>
        </div>

        {approvedTeams.length === 0 ? (
          <p className="page-note">
            Сначала подтвердите хотя бы одну заявку, чтобы открыть чат с капитаном.
          </p>
        ) : (
          <div className="stack">
            <label className="field">
              <span>Команда</span>
              <select value={effectiveSelectedTeamId} onChange={(event) => setSelectedTeamId(event.target.value)}>
                {approvedTeams.map((team) => (
                  <option key={team.teamId} value={team.teamId}>
                    {team.teamName}
                  </option>
                ))}
              </select>
            </label>

            {isCaptainOrganizerChatLoading ? <p className="page-note">Загрузка сообщений...</p> : null}
            {captainOrganizerChatError?.message ? (
              <p className="form-message form-message--error">{captainOrganizerChatError.message}</p>
            ) : null}
            {chatError ? <p className="form-message form-message--error">{chatError}</p> : null}

            <div ref={chatWindowRef} className="chat-window">
              {chatFeed.length === 0 ? (
                <p className="page-note">Сообщений пока нет.</p>
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
                value={chatText}
                onChange={(event) => setChatText(event.target.value)}
                onInput={(event) => resizeChatInput(event.target)}
                onKeyDown={handleChatInputKeyDown}
                placeholder="Напиши капитану команды"
              />
              <button
                className="button button--primary"
                type="button"
                onClick={handleSendMessage}
                disabled={
                  isSendingMessage ||
                  isAwaitingChatAck ||
                  !chatText.trim() ||
                  !effectiveSelectedTeamId
                }
              >
                {isSendingMessage || isAwaitingChatAck ? 'Отправляем...' : 'Отправить'}
              </button>
            </div>
          </div>
        )}
      </section>
    </section>
  )
}
