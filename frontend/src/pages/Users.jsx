import { useState, useEffect, useCallback } from 'react'
import { userApi } from '../api/index.js'
import Modal from '../components/Modal.jsx'

export default function Users() {
  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [editingRoles, setEditingRoles] = useState(null)
  const [selectedRoleIds, setSelectedRoleIds] = useState([])

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const params = { pageNum, pageSize: 10 }
      if (keyword.trim()) params.keyword = keyword.trim()
      const res = await userApi.page(params)
      if (res.code === 200) { setUsers(res.data.records || []); setTotal(res.data.total || 0) }
      else setError(res.message)
    } catch (e) { setError(e.message) }
    finally { setLoading(false) }
  }, [pageNum, keyword])

  const fetchRoles = async () => {
    try {
      const res = await userApi.allRoles()
      if (res.code === 200) setRoles(res.data)
    } catch (e) { /* ignore */ }
  }

  useEffect(() => { fetchUsers() }, [fetchUsers])
  useEffect(() => { fetchRoles() }, [])

  const handleSearch = (e) => { e.preventDefault(); setPageNum(1); fetchUsers() }

  const handleToggle = async (user) => {
    setError(''); setSuccess('')
    try {
      const res = user.status === 1 ? await userApi.disable(user.id) : await userApi.enable(user.id)
      if (res.code === 200) { setSuccess(`用户 ${user.username} 已${user.status === 1 ? '禁用' : '启用'}`); fetchUsers() }
      else setError(res.message)
    } catch (e) { setError(e.message) }
  }

  const openRoleEditor = (user) => {
    setEditingRoles(user)
    setSelectedRoleIds([])
  }

  const handleAssignRoles = async () => {
    if (!editingRoles) return
    setError(''); setSuccess('')
    try {
      const res = await userApi.assignRoles(editingRoles.id, selectedRoleIds)
      if (res.code === 200) { setSuccess('角色分配成功'); setEditingRoles(null); fetchUsers() }
      else setError(res.message)
    } catch (e) { setError(e.message) }
  }

  const totalPages = Math.ceil(total / 10)

  return (
    <div className="page-wide">
      <div className="page-header"><h2>用户管理</h2></div>
      {error && <div className="error-msg">{error}</div>}
      {success && <div className="success-msg">{success}</div>}

      <form className="search-bar" onSubmit={handleSearch}>
        <input type="text" placeholder="搜索用户名/昵称" value={keyword} onChange={e => setKeyword(e.target.value)} style={{ flex: 1 }} />
        <button type="submit" className="btn">搜索</button>
      </form>

      {editingRoles && (
        <Modal title={`分配角色 - ${editingRoles.username}`} onClose={() => setEditingRoles(null)}>
          {error && <div className="error-msg">{error}</div>}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginBottom: 20 }}>
            {roles.map(r => (
              <label key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 14 }}>
                <input type="checkbox" value={r.id}
                  checked={selectedRoleIds.includes(r.id)}
                  onChange={e => {
                    if (e.target.checked) setSelectedRoleIds([...selectedRoleIds, r.id])
                    else setSelectedRoleIds(selectedRoleIds.filter(id => id !== r.id))
                  }} />
                {r.roleName}
              </label>
            ))}
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button className="btn btn-outline" onClick={() => setEditingRoles(null)}>取消</button>
            <button className="btn" onClick={handleAssignRoles}>保存</button>
          </div>
        </Modal>
      )}

      {loading ? <div className="card" style={{ textAlign: 'center' }}>加载中...</div> : (
        <div className="table-wrap">
          <table>
            <thead><tr><th>ID</th><th>用户名</th><th>昵称</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {users.length === 0 ? (
                <tr><td colSpan={5} style={{ textAlign: 'center', color: '#999' }}>暂无数据</td></tr>
              ) : users.map(u => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td><strong>{u.username}</strong></td>
                  <td>{u.nickname || '-'}</td>
                  <td><span className={`tag ${u.status === 1 ? '' : 'tag-danger'}`}>{u.status === 1 ? '正常' : '已禁用'}</span></td>
                  <td>
                    <button className="btn-link" onClick={() => openRoleEditor(u)}>分配角色</button>
                    <button className={`btn-link ${u.status === 1 ? 'btn-danger' : ''}`} onClick={() => handleToggle(u)}>
                      {u.status === 1 ? '禁用' : '启用'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-sm" disabled={pageNum <= 1} onClick={() => setPageNum(p => p - 1)}>上一页</button>
          <span className="page-info">第 {pageNum} / {totalPages} 页，共 {total} 条</span>
          <button className="btn btn-sm" disabled={pageNum >= totalPages} onClick={() => setPageNum(p => p + 1)}>下一页</button>
        </div>
      )}
    </div>
  )
}
