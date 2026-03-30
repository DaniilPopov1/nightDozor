import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import { fetchCurrentUser, loginRequest, registerRequest } from './authApi.js'

const TOKEN_STORAGE_KEY = 'nightdozor_access_token'

function getInitialToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY)
}

const initialState = {
  token: getInitialToken(),
  user: null,
  loginStatus: 'idle',
  registerStatus: 'idle',
  profileStatus: 'idle',
  loginError: null,
  registerError: null,
  profileError: null,
  registerMessage: null,
}

export const loginUser = createAsyncThunk(
  'auth/loginUser',
  async (credentials, { rejectWithValue }) => {
    try {
      return await loginRequest(credentials)
    } catch (error) {
      return rejectWithValue(error.message)
    }
  },
)

export const registerUser = createAsyncThunk(
  'auth/registerUser',
  async (payload, { rejectWithValue }) => {
    try {
      return await registerRequest(payload)
    } catch (error) {
      return rejectWithValue(error.message)
    }
  },
)

export const loadCurrentUser = createAsyncThunk(
  'auth/loadCurrentUser',
  async (_, { getState, rejectWithValue }) => {
    const token = getState().auth.token

    if (!token) {
      return rejectWithValue('Токен отсутствует')
    }

    try {
      return await fetchCurrentUser(token)
    } catch (error) {
      return rejectWithValue(error.message)
    }
  },
)

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearAuthFeedback(state) {
      state.loginError = null
      state.registerError = null
      state.profileError = null
      state.registerMessage = null
    },
    logout(state) {
      state.token = null
      state.user = null
      state.loginStatus = 'idle'
      state.profileStatus = 'idle'
      state.loginError = null
      state.profileError = null
      localStorage.removeItem(TOKEN_STORAGE_KEY)
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.loginStatus = 'loading'
        state.loginError = null
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.loginStatus = 'succeeded'
        state.token = action.payload.accessToken
        state.user = {
          email: action.payload.email,
          role: action.payload.role,
        }
        localStorage.setItem(TOKEN_STORAGE_KEY, action.payload.accessToken)
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loginStatus = 'failed'
        state.loginError = action.payload || 'Не удалось выполнить вход'
      })
      .addCase(registerUser.pending, (state) => {
        state.registerStatus = 'loading'
        state.registerError = null
        state.registerMessage = null
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        state.registerStatus = 'succeeded'
        state.registerMessage = action.payload.message || 'Регистрация выполнена'
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.registerStatus = 'failed'
        state.registerError = action.payload || 'Не удалось выполнить регистрацию'
      })
      .addCase(loadCurrentUser.pending, (state) => {
        state.profileStatus = 'loading'
        state.profileError = null
      })
      .addCase(loadCurrentUser.fulfilled, (state, action) => {
        state.profileStatus = 'succeeded'
        state.user = action.payload
      })
      .addCase(loadCurrentUser.rejected, (state, action) => {
        state.profileStatus = 'failed'
        state.profileError = action.payload || 'Не удалось загрузить профиль'
      })
  },
})

export const { clearAuthFeedback, logout } = authSlice.actions
export default authSlice.reducer
