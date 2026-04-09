import { createSlice } from '@reduxjs/toolkit'
import { apiSlice } from '../../shared/api/apiSlice.js'

const TOKEN_STORAGE_KEY = 'nightdozor_access_token'

function getInitialToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

const initialState = {
  token: getInitialToken(),
  user: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials(state, action) {
      state.token = action.payload.accessToken
      state.user = {
        email: action.payload.email,
        role: action.payload.role,
      }
      localStorage.setItem(TOKEN_STORAGE_KEY, action.payload.accessToken)
    },
    logout(state) {
      state.token = null
      state.user = null
      localStorage.removeItem(TOKEN_STORAGE_KEY)
    },
  },
  extraReducers: (builder) => {
    builder
      .addMatcher(
        apiSlice.endpoints.getCurrentUser.matchFulfilled,
        (state, action) => {
          state.user = action.payload
        },
      )
      .addMatcher(
        apiSlice.endpoints.getCurrentUser.matchRejected,
        (state) => {
          state.user = null
        },
      )
      .addMatcher(apiSlice.util.resetApiState.match, (state) => {
        if (!state.token) {
          state.user = null
        }
      })
  },
})

export const { logout, setCredentials } = authSlice.actions
export default authSlice.reducer
