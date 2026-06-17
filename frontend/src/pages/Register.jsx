import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authApi } from '../api/index.js'

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '', confirmPassword: '', nickname: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')

    if (form.password !== form.confirmPassword) {
      setError('两次输入的密码不一致')
      return
    }

    setLoading(true)
    try {
      const res = await authApi.register(form.username, form.password, form.nickname)
      if (res.code === 200) {
        setSuccess('注册成功！即将跳转到登录页...')
        setTimeout(() => navigate('/login'), 1500)
      } else {
        setError(res.message)
      }
    } catch (err) {
      setError(err.message || '注册失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <h2>注册</h2>
      {error && <div className="error-msg">{error}</div>}
      {success && <div className="success-msg">{success}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>用户名</label>
          <input
            type="text"
            placeholder="3-50个字符"
            value={form.username}
            onChange={e => setForm({ ...form, username: e.target.value })}
            required
          />
        </div>
        <div className="form-group">
          <label>昵称</label>
          <input
            type="text"
            placeholder="选填"
            value={form.nickname}
            onChange={e => setForm({ ...form, nickname: e.target.value })}
          />
        </div>
        <div className="form-group">
          <label>密码</label>
          <input
            type="password"
            placeholder="至少6个字符"
            value={form.password}
            onChange={e => setForm({ ...form, password: e.target.value })}
            required
          />
        </div>
        <div className="form-group">
          <label>确认密码</label>
          <input
            type="password"
            placeholder="再次输入密码"
            value={form.confirmPassword}
            onChange={e => setForm({ ...form, confirmPassword: e.target.value })}
            required
          />
        </div>
        <button type="submit" className="btn btn-full" disabled={loading}>
          {loading ? '注册中...' : '注册'}
        </button>
      </form>
      <div className="link-text">
        已有账号？<Link to="/login">去登录</Link>
      </div>
    </div>
  )
}
