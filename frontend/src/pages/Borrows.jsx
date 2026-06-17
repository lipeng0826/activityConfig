import { useState, useEffect, useCallback } from 'react'
import { borrowApi, bookApi } from '../api/index.js'
import Modal from '../components/Modal.jsx'

export default function Borrows() {
  const [borrows, setBorrows] = useState([])
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showBorrowModal, setShowBorrowModal] = useState(false)
  const [borrowBookId, setBorrowBookId] = useState('')

  const fetchBorrows = useCallback(async () => {
    try {
      const res = await borrowApi.myBorrows()
      if (res.code === 200) setBorrows(res.data)
      else setError(res.message)
    } catch (e) { setError(e.message) }
    finally { setLoading(false) }
  }, [])

  const fetchBooks = async () => {
    try {
      const res = await bookApi.page({ pageNum: 1, pageSize: 100 })
      if (res.code === 200) setBooks(res.data.records || [])
    } catch (e) { /* ignore */ }
  }

  useEffect(() => { fetchBorrows(); fetchBooks() }, [fetchBorrows])

  const handleBorrow = async () => {
    if (!borrowBookId) return
    setError(''); setSuccess('')
    try {
      const res = await borrowApi.borrow(Number(borrowBookId))
      if (res.code === 200) { setSuccess('借阅成功！'); fetchBorrows(); setBorrowBookId(''); setShowBorrowModal(false) }
      else setError(res.message)
    } catch (e) { setError(e.message) }
  }

  const handleReturn = async (id) => {
    setError(''); setSuccess('')
    try {
      const res = await borrowApi.return(id)
      if (res.code === 200) { setSuccess(res.data.status === 2 ? '归还成功（已逾期）' : '归还成功！'); fetchBorrows() }
      else setError(res.message)
    } catch (e) { setError(e.message) }
  }

  const handleRenew = async (id) => {
    setError(''); setSuccess('')
    try {
      const res = await borrowApi.renew(id)
      if (res.code === 200) { setSuccess('续借成功！'); fetchBorrows() }
      else setError(res.message)
    } catch (e) { setError(e.message) }
  }

  const statusText = (s) => ['借阅中', '已归还', '逾期归还'][s] || '未知'
  const statusClass = (s) => s === 0 ? 'tag-role' : s === 2 ? 'tag-danger' : 'tag'
  const isOverdue = (r) => r.status === 0 && new Date(r.dueTime) < new Date()
  const bookTitle = (id) => books.find(b => b.id === id)?.title || `ID:${id}`
  const fmt = (t) => t ? new Date(t).toLocaleDateString('zh-CN') : '-'

  if (loading) return <div className="card" style={{ textAlign: 'center' }}>加载中...</div>

  return (
    <div className="page-wide">
      <div className="page-header">
        <h2>我的借阅</h2>
        <button className="btn" onClick={() => { setBorrowBookId(''); setError(''); setShowBorrowModal(true) }}>+ 借阅新书</button>
      </div>

      {error && <div className="error-msg">{error}</div>}
      {success && <div className="success-msg">{success}</div>}

      {showBorrowModal && (
        <Modal title="借阅新书" onClose={() => setShowBorrowModal(false)}>
          {error && <div className="error-msg">{error}</div>}
          <div className="form-group">
            <label>选择图书</label>
            <select value={borrowBookId} onChange={e => setBorrowBookId(e.target.value)}>
              <option value="">请选择...</option>
              {books.filter(b => b.availableCopies > 0).map(b => (
                <option key={b.id} value={b.id}>{b.title} (可借: {b.availableCopies})</option>
              ))}
            </select>
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 20 }}>
            <button className="btn btn-outline" onClick={() => setShowBorrowModal(false)}>取消</button>
            <button className="btn" onClick={handleBorrow} disabled={!borrowBookId}>确认借书</button>
          </div>
        </Modal>
      )}

      {/* 借阅记录列表 */}
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>图书</th><th>借阅时间</th><th>应还时间</th><th>归还时间</th><th>续借</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            {borrows.length === 0 ? (
              <tr><td colSpan={7} style={{ textAlign: 'center', color: '#999' }}>暂无借阅记录</td></tr>
            ) : borrows.map(r => (
              <tr key={r.id}>
                <td><strong>{bookTitle(r.bookId)}</strong></td>
                <td>{fmt(r.borrowTime)}</td>
                <td style={{ color: isOverdue(r) ? '#e53e3e' : 'inherit' }}>{fmt(r.dueTime)}{isOverdue(r) ? ' ⚠️' : ''}</td>
                <td>{fmt(r.returnTime)}</td>
                <td>{r.renewCount}/1</td>
                <td><span className={`tag ${statusClass(r.status)}`}>{statusText(r.status)}</span></td>
                <td>
                  {r.status === 0 && (
                    <>
                      <button className="btn-link" onClick={() => handleReturn(r.id)}>还书</button>
                      {r.renewCount < 1 && !isOverdue(r) && (
                        <button className="btn-link" onClick={() => handleRenew(r.id)}>续借</button>
                      )}
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
