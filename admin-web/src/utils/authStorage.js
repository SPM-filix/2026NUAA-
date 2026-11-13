const STORAGE_KEY = 'nuaa_admin_web_auth'

export function loadAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function saveAuth(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

export function clearAuthStorage() {
  localStorage.removeItem(STORAGE_KEY)
}

export function getToken() {
  return loadAuth()?.token || ''
}
