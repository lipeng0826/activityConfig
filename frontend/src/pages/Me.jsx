import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/index.js'

export default function Me() {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) {
      navigate('/login')
      return
    }

    authApi.getMe()
      .then(res => {
        if (res.code === 200) {
          setUser(res.data)
        } else {
          setError(res.message)
          localStorage.removeItem('token')
        }
      })
      .catch(err => {
        setError(err.message || '获取用户信息失败')
        localStorage.removeItem('token')
      })
      .finally(() => setLoading(false))
  }, [navigate])

  if (loading) return <div className="card" style={{ textAlign: 'center' }}>加载中...</div>
  if (error) return <div className="card"><div className="error-msg">{error}</div></div>
  if (!user) return null

  return (
    <div className="card">
      <h2>个人中心</h2>
      <ul className="user-info">
        <li>
          <span className="label">ID</span>
          <span className="value">{user.id}</span>
        </li>
        <li>
          <span className="label">用户名</span>
          <span className="value">{user.username}</span>
        </li>
        <li>
          <span className="label">昵称</span>
          <span className="value">{user.nickname || '-'}</span>
        </li>
        <li>
          <span className="label">邮箱</span>
          <span className="value">{user.email || '-'}</span>
        </li>
        <li>
          <span className="label">手机号</span>
          <span className="value">{user.phone || '-'}</span>
        </li>
        <li>
          <span className="label">角色</span>
          <span className="value">
            <div className="tag-list">
              {user.roles?.map(r => <span key={r} className="tag tag-role">{r}</span>)}
            </div>
          </span>
        </li>
        <li>
          <span className="label">权限</span>
          <span className="value">
            <div className="tag-list">
              {user.permissions?.map(p => <span key={p} className="tag">{p}</span>)}
            </div>
          </span>
        </li>
      </ul>
    </div>
  )
}
