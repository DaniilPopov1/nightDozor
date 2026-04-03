import { apiSlice } from '../../shared/api/apiSlice.js'

export const teamApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getCurrentTeam: builder.query({
      query: () => '/teams/me',
      providesTags: ['CurrentTeam', 'TeamMembership'],
    }),
    getTeamById: builder.query({
      query: (teamId) => `/teams/${teamId}`,
      providesTags: (result, error, teamId) => [{ type: 'Team', id: teamId }],
    }),
    getTeams: builder.query({
      query: (city = '') => ({
        url: '/teams',
        params: city ? { city } : undefined,
      }),
      providesTags: ['Teams'],
    }),
    getOutgoingJoinRequests: builder.query({
      query: () => '/teams/me/outgoing-join-requests',
      providesTags: ['OutgoingJoinRequests'],
    }),
    getIncomingJoinRequests: builder.query({
      query: () => '/teams/me/join-requests',
      providesTags: ['IncomingJoinRequests'],
    }),
    createTeam: builder.mutation({
      query: (payload) => ({
        url: '/teams',
        method: 'POST',
        body: payload,
      }),
      invalidatesTags: ['CurrentTeam', 'TeamMembership', 'Teams'],
    }),
    joinTeamByCode: builder.mutation({
      query: (inviteCode) => ({
        url: '/teams/join-by-code',
        method: 'POST',
        body: { inviteCode },
      }),
      invalidatesTags: ['CurrentTeam', 'TeamMembership', 'OutgoingJoinRequests', 'Teams'],
    }),
    createJoinRequest: builder.mutation({
      query: (teamId) => ({
        url: `/teams/${teamId}/join-requests`,
        method: 'POST',
      }),
      invalidatesTags: ['OutgoingJoinRequests', 'Teams'],
    }),
    leaveTeam: builder.mutation({
      query: () => ({
        url: '/teams/leave',
        method: 'POST',
      }),
      invalidatesTags: [
        'CurrentTeam',
        'TeamMembership',
        'IncomingJoinRequests',
        'OutgoingJoinRequests',
        'Teams',
        'MyTeamRegistrations',
        'Games',
      ],
    }),
  }),
})

export const {
  useCreateJoinRequestMutation,
  useCreateTeamMutation,
  useGetCurrentTeamQuery,
  useGetIncomingJoinRequestsQuery,
  useGetOutgoingJoinRequestsQuery,
  useGetTeamByIdQuery,
  useGetTeamsQuery,
  useJoinTeamByCodeMutation,
  useLeaveTeamMutation,
} = teamApi
