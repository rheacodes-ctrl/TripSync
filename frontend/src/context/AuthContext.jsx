import { createContext, useContext, useState } from 'react'
import { api } from '../api/client.js'

const Ctx = createContext(null)
export const useAuth = () => useContext(Ctx)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('tripsync-user')) } catch { return null }
  })
  const store = (u) => { localStorage.setItem('tripsync-user', JSON.stringify(u)); setUser(u) }
  const finish = ({ user, token }) => { localStorage.setItem('tripsync-token', token || ''); store(user) }
  const login = async (email, password) => finish(await api.login(email, password))
  const register = async (data) => finish(await api.register(data))
  const updateProfile = async (data) => store({ ...user, ...(await api.updateProfile(user.userId, data)) })
  const logout = () => {
    localStorage.removeItem('tripsync-user'); localStorage.removeItem('tripsync-token'); setUser(null)
  }
  return <Ctx.Provider value={{ user, login, register, updateProfile, logout }}>{children}</Ctx.Provider>
}
