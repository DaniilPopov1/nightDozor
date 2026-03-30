const BASE_HEADERS = {
  'Content-Type': 'application/json',
}

async function parseResponse(response) {
  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    if (typeof data === 'string' && data.trim()) {
      throw new Error(data)
    }

    if (data?.error) {
      throw new Error(data.error)
    }

    if (data?.message) {
      throw new Error(data.message)
    }

    throw new Error('Не удалось выполнить запрос')
  }

  return data
}

export async function loginRequest(payload) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: BASE_HEADERS,
    body: JSON.stringify(payload),
  })

  return parseResponse(response)
}

export async function registerRequest(payload) {
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: BASE_HEADERS,
    body: JSON.stringify(payload),
  })

  return parseResponse(response)
}

export async function fetchCurrentUser(token) {
  const response = await fetch('/api/auth/me', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })

  return parseResponse(response)
}
