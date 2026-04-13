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
    getOrganizerGames: builder.query({
      query: () => '/games/my',
      providesTags: ['OrganizerGames'],
    }),
    getOrganizerGameById: builder.query({
      query: (gameId) => `/games/my/${gameId}`,
      providesTags: (result, error, gameId) => [{ type: 'OrganizerGame', id: gameId }],
    }),
    createGame: builder.mutation({
      query: (payload) => ({
        url: '/games',
        method: 'POST',
        body: payload,
      }),
      invalidatesTags: ['OrganizerGames', 'Games'],
    }),
    updateGame: builder.mutation({
      query: ({ gameId, payload }) => ({
        url: `/games/my/${gameId}`,
        method: 'PUT',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [
        'OrganizerGames',
        'Games',
        { type: 'OrganizerGame', id: gameId },
      ],
    }),
    cancelOrganizerGame: builder.mutation({
      query: (gameId) => ({
        url: `/games/my/${gameId}/status`,
        method: 'POST',
      }),
      invalidatesTags: (result, error, gameId) => [
        'OrganizerGames',
        'Games',
        'MyTeamRegistrations',
        { type: 'OrganizerGame', id: gameId },
      ],
    }),
    getIncomingRegistrations: builder.query({
      query: (gameId) => `/games/my/${gameId}/registrations`,
      providesTags: (result, error, gameId) => [{ type: 'IncomingRegistrations', id: gameId }],
    }),
    getOrganizerGameTasks: builder.query({
      query: (gameId) => `/games/my/${gameId}/tasks`,
      providesTags: (result, error, gameId) => [{ type: 'OrganizerTasks', id: gameId }],
    }),
    getOrganizerGameRoutes: builder.query({
      query: (gameId) => `/games/my/${gameId}/routes`,
      providesTags: (result, error, gameId) => [{ type: 'OrganizerRoutes', id: gameId }],
    }),
    approveRegistration: builder.mutation({
      query: ({ gameId, registrationId }) => ({
        url: `/games/my/${gameId}/registrations/${registrationId}/approve`,
        method: 'POST',
      }),
      invalidatesTags: (result, error, { gameId }) => [
        { type: 'IncomingRegistrations', id: gameId },
        { type: 'OrganizerRoutes', id: gameId },
        { type: 'OrganizerGame', id: gameId },
        'MyTeamRegistrations',
      ],
    }),
    rejectRegistration: builder.mutation({
      query: ({ gameId, registrationId }) => ({
        url: `/games/my/${gameId}/registrations/${registrationId}/reject`,
        method: 'POST',
      }),
      invalidatesTags: (result, error, { gameId }) => [
        { type: 'IncomingRegistrations', id: gameId },
        { type: 'OrganizerGame', id: gameId },
        'MyTeamRegistrations',
      ],
    }),
    createTask: builder.mutation({
      query: ({ gameId, payload }) => ({
        url: `/games/my/${gameId}/tasks`,
        method: 'POST',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [
        { type: 'OrganizerTasks', id: gameId },
        { type: 'OrganizerRoutes', id: gameId },
      ],
    }),
    updateTask: builder.mutation({
      query: ({ gameId, taskId, payload }) => ({
        url: `/games/my/${gameId}/tasks/${taskId}`,
        method: 'PUT',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [
        { type: 'OrganizerTasks', id: gameId },
        { type: 'OrganizerRoutes', id: gameId },
      ],
    }),
    deleteTask: builder.mutation({
      query: ({ gameId, taskId }) => ({
        url: `/games/my/${gameId}/tasks/${taskId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { gameId }) => [
        { type: 'OrganizerTasks', id: gameId },
        { type: 'OrganizerRoutes', id: gameId },
      ],
    }),
    createTaskHint: builder.mutation({
      query: ({ gameId, taskId, payload }) => ({
        url: `/games/my/${gameId}/tasks/${taskId}/hints`,
        method: 'POST',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerTasks', id: gameId }],
    }),
    updateTaskHint: builder.mutation({
      query: ({ gameId, taskId, hintId, payload }) => ({
        url: `/games/my/${gameId}/tasks/${taskId}/hints/${hintId}`,
        method: 'PUT',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerTasks', id: gameId }],
    }),
    deleteTaskHint: builder.mutation({
      query: ({ gameId, taskId, hintId }) => ({
        url: `/games/my/${gameId}/tasks/${taskId}/hints/${hintId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerTasks', id: gameId }],
    }),
    createRoute: builder.mutation({
      query: ({ gameId, payload }) => ({
        url: `/games/my/${gameId}/routes`,
        method: 'POST',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerRoutes', id: gameId }],
    }),
    updateRoute: builder.mutation({
      query: ({ gameId, routeId, payload }) => ({
        url: `/games/my/${gameId}/routes/${routeId}`,
        method: 'PUT',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerRoutes', id: gameId }],
    }),
    deleteRoute: builder.mutation({
      query: ({ gameId, routeId }) => ({
        url: `/games/my/${gameId}/routes/${routeId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerRoutes', id: gameId }],
    }),
    addTaskToRoute: builder.mutation({
      query: ({ gameId, routeId, payload }) => ({
        url: `/games/my/${gameId}/routes/${routeId}/items`,
        method: 'POST',
        body: payload,
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerRoutes', id: gameId }],
    }),
    removeTaskFromRoute: builder.mutation({
      query: ({ gameId, routeId, itemId }) => ({
        url: `/games/my/${gameId}/routes/${routeId}/items/${itemId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { gameId }) => [{ type: 'OrganizerRoutes', id: gameId }],
    }),
  }),
})

export const {
  useAddTaskToRouteMutation,
  useApproveRegistrationMutation,
  useCancelGameRegistrationMutation,
  useCreateGameMutation,
  useCreateRouteMutation,
  useCreateTaskHintMutation,
  useCreateTaskMutation,
  useDeleteTaskHintMutation,
  useDeleteRouteMutation,
  useDeleteTaskMutation,
  useGetGamesQuery,
  useGetIncomingRegistrationsQuery,
  useGetMyTeamRegistrationsQuery,
  useGetOrganizerGameByIdQuery,
  useGetOrganizerGamesQuery,
  useGetOrganizerGameRoutesQuery,
  useGetOrganizerGameTasksQuery,
  useRemoveTaskFromRouteMutation,
  useRejectRegistrationMutation,
  useSubmitGameRegistrationMutation,
  useUpdateGameMutation,
  useUpdateRouteMutation,
  useUpdateTaskHintMutation,
  useUpdateTaskMutation,
  useCancelOrganizerGameMutation,
} = gameApi
