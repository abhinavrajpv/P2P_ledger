/**
 * Auth helpers — uses sessionStorage so each browser tab has its own session.
 * This prevents multi-tab session bleed when two accounts are open simultaneously.
 */

export const getToken = () => sessionStorage.getItem('token')
export const getUserId = () => sessionStorage.getItem('userId')
export const getUser = () => {
    try { return JSON.parse(sessionStorage.getItem('user')) } catch { return null }
}

export const setAuth = (token, user) => {
    sessionStorage.setItem('token', token)
    sessionStorage.setItem('userId', String(user.id))
    sessionStorage.setItem('user', JSON.stringify(user))
}

export const clearAuth = () => {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('user')
}

export const isLoggedIn = () => !!sessionStorage.getItem('token')
