const TOKEN_KEY = 'nuaa.user.token'
const USER_KEY = 'nuaa.user.profile'
const MUST_CHANGE_KEY = 'nuaa.user.mustChangePassword'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export function getMustChangePassword() {
  return localStorage.getItem(MUST_CHANGE_KEY) === '1'
}

export function saveSession({ token, user, mustChangePassword }) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  if (mustChangePassword !== undefined) {
    localStorage.setItem(MUST_CHANGE_KEY, mustChangePassword ? '1' : '0')
  }
}

export function saveUser(user) {
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(MUST_CHANGE_KEY)
}
