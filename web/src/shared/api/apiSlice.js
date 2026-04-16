import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

const rawBaseQuery = fetchBaseQuery({
  baseUrl: '/api',
  prepareHeaders: (headers, { getState }) => {
    const token = getState().auth.token

    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }

    return headers
  },
})

const baseQuery = async (args, api, extraOptions) => {
  const result = await rawBaseQuery(args, api, extraOptions)

  if (result.error) {
    const message =
      typeof result.error.data === 'string'
        ? result.error.data
        : result.error.data?.error || result.error.data?.message || 'Не удалось выполнить запрос'

    return {
      error: {
        ...result.error,
        message,
      },
    }
  }

  return result
}

export const apiSlice = createApi({
  reducerPath: 'api',
  baseQuery,
  tagTypes: [
    'CurrentUser',
    'CurrentTeam',
    'TeamMembership',
    'Teams',
    'Team',
    'IncomingJoinRequests',
    'OutgoingJoinRequests',
    'Games',
    'MyTeamRegistrations',
    'OrganizerGames',
    'OrganizerGame',
    'IncomingRegistrations',
    'OrganizerTasks',
    'OrganizerRoutes',
    'TeamChat',
    'CaptainOrganizerChat',
  ],
  endpoints: () => ({}),
})
