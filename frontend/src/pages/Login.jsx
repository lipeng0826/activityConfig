import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '../api/index.js'

export default function Login() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await authApi.login(form.username.trim(), form.password)
      if (res.code === 200) {
        localStorage.setItem('token', res.data)
        navigate('/me')
      } else {
        setError(res.message)
      }
    } catch (err) {
      setError(err.message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <h2>登录</h2>
      {error && <div className="error-msg">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>用户名</label>
          <input
            type="text"
            placeholder="请输入用户名"
            value={form.username}
            onChange={e => setForm({ ...form, username: e.target.value })}
            required
          />
        </div>
        <div className="form-group">
          <label>密码</label>
          <input
            type="password"
            placeholder="请输入密码"
            value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })}
            required
          />
        </div>
        <button type="submit" className="btn btn-full" disabled={loading}>
          {loading ? '登录中...' : '登录'}
        </button>
      </form>
      <div className="link-text">
        没有账号？<Link to="/register">立即注册</Link>
      </div>
    </div>
  )
}
