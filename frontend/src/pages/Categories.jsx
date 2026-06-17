import { useState, useEffect } from 'react'
import { categoryApi } from '../api/index.js'
import Modal from '../components/Modal.jsx'

export default function Categories() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null) // null=新增 | category=编辑
  const [form, setForm] = useState({ name: '', description: '' })

  const fetchList = async () => {
    try {
      const res = await categoryApi.list()
      if (res.code === 200) setList(res.data)
      else setError(res.message)
    } catch (e) { setError(e.message || '加载失败') }
    finally { setLoading(false) }
  }

  useEffect(() => { fetchList() }, [])

  const openCreate = () => {
    setEditing(null)
    setForm({ name: '', description: '' })
    setShowModal(true)
  }

  const openEdit = (item) => {
    setEditing(item)
    setForm({ name: item.name, description: item.description || '' })
    setShowModal(true)
  }

  const closeModal = () => { setShowModal(false); setError('') }

  const handleSave = async () => {
    if (!form.name.trim()) { setError('分类名称不能为空'); return }
    try {
      if (!editing) {
        const res = await categoryApi.create(form)
        if (res.code !== 200) { setError(res.message); return }
      } else {
        const res = await categoryApi.update(editing.id, form)
        if (res.code !== 200) { setError(res.message); return }
      }
      closeModal()
      fetchList()
    } catch (e) { setError(e.message || '保存失败') }
  }

  const handleDelete = async (id, name) => {
    if (!confirm(`确定删除分类「${name}」吗？`)) return
    try {
      const res = await categoryApi.delete(id)
      if (res.code === 200) fetchList()
      else setError(res.message)
    } catch (e) { setError(e.message || '删除失败') }
  }

  if (loading) return <div className="card" style={{ textAlign: 'center' }}>加载中...</div>

  return (
    <div className="page-wide">
      <div className="page-header">
        <h2>分类管理</h2>
        <button className="btn" onClick={openCreate}>+ 新增分类</button>
      </div>

      {showModal && (
        <Modal title={editing ? '编辑分类' : '新增分类'} onClose={closeModal}>
          {error && <div className="error-msg">{error}</div>}
          <div className="form-group">
            <label>名称</label>
            <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="分类名称" autoFocus />
          </div>
          <div className="form-group">
            <label>描述</label>
            <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="分类描述（选填）" />
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 20 }}>
            <button className="btn btn-outline" onClick={closeModal}>取消</button>
            <button className="btn" onClick={handleSave}>保存</button>
          </div>
        </Modal>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>ID</th><th>名称</th><th>描述</th><th>操作</th></tr>
          </thead>
          <tbody>
            {list.length === 0 ? (
              <tr><td colSpan={4} style={{ textAlign: 'center', color: '#999' }}>暂无数据</td></tr>
            ) : list.map(item => (
              <tr key={item.id}>
                <td>{item.id}</td>
                <td><strong>{item.name}</strong></td>
                <td style={{ color: '#888' }}>{item.description || '-'}</td>
                <td>
                  <button className="btn-link" onClick={() => openEdit(item)}>编辑</button>
                  <button className="btn-link btn-danger" onClick={() => handleDelete(item.id, item.name)}>删除</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
