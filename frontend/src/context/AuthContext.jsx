import { createContext, useContext, useState } from 'react'
import { api } from '../api/client.js'

const Ctx = createContext(null)
export const useAuth = () => useContext(Ctx)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('tripsync-user')) } catch { return null }
  })
  const finish = ({ user, token }) => {
    localStorage.setItem('tripsync-user', JSON.stringify(user))
    localStorage.setItem('tripsync-token', token || '')
    setUser(user)
  }
  const login = async (email, password) => finish(await api.login(email, password))
  const register = async (data) => finish(await api.register(data))
  const logout = () => {
    localStorage.removeItem('tripsync-user'); localStorage.removeItem('tripsync-token'); setUser(null)
  }
  return <Ctx.Provider value={{ user, login, register, logout }}>{children}</Ctx.Provider>
}
