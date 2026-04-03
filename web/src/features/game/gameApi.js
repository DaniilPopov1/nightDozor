import { apiSlice } from '../../shared/api/apiSlice.js'

export const gameApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getGames: builder.query({
      query: (city = '') => ({
        url: '/games',
        params: city ? { city } : undefined,
      }),
      providesTags: ['Games'],
    }),
    getMyTeamRegistrations: builder.query({
      query: () => '/games/registrations/my-team',
      providesTags: ['MyTeamRegistrations'],
    }),
    submitGameRegistration: builder.mutation({
      query: (gameId) => ({
        url: `/games/${gameId}/registrations`,
        method: 'POST',
      }),
      invalidatesTags: ['MyTeamRegistrations', 'Games'],
    }),
    cancelGameRegistration: builder.mutation({
      query: (gameId) => ({
        url: `/games/${gameId}/registrations/cancel`,
        method: 'POST',
      }),
      invalidatesTags: ['MyTeamRegistrations', 'Games'],
    }),
  }),
})

export const {
  useCancelGameRegistrationMutation,
  useGetGamesQuery,
  useGetMyTeamRegistrationsQuery,
  useSubmitGameRegistrationMutation,
} = gameApi
