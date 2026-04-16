export function buildChatSocketUrl(token) {
  const encodedToken = encodeURIComponent(token)

  if (import.meta.env.DEV) {
    return `ws://localhost:8080/ws/chat?token=${encodedToken}`
  }

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${window.location.host}/ws/chat?token=${encodedToken}`
}

export function parseSocketEvent(rawEvent) {
  try {
    return JSON.parse(rawEvent.data)
  } catch {
    return null
  }
}
