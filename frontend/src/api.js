// Minimal API client for the insurance backend.
// API base URL: set VITE_API_URL, otherwise http://localhost:8080.
// Auth: HTTP Basic. Credentials live only in sessionStorage for this browser
// tab (demo-only setup — see the main README "Security (demo only)").

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

function authHeader() {
  const creds = sessionStorage.getItem('insurance-auth')
  return creds ? { Authorization: 'Basic ' + creds } : {}
}

export async function api(path, method = 'GET', body) {
  const res = await fetch(API_URL + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let data
  try {
    data = JSON.parse(text)
  } catch {
    data = text
  }
  if (!res.ok) {
    throw new Error((data && data.message) || `Request failed: ${res.status}`)
  }
  return data
}

export function login(username, password) {
  sessionStorage.setItem('insurance-auth', btoa(`${username}:${password}`))
}

export function logout() {
  sessionStorage.removeItem('insurance-auth')
}

export function isLoggedIn() {
  return !!sessionStorage.getItem('insurance-auth')
}
