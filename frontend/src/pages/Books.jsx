import { useState, useEffect, useCallback } from 'react'
import { bookApi, categoryApi } from '../api/index.js'
import Modal from '../components/Modal.jsx'

const emptyForm = {
  title: '', author: '', isbn: '', publisher: '',
  price: '', stock: '', categoryId: '', coverUrl: '', description: ''
}

export default function Books() {
  const [books, setBooks] = useState([])
  const [categories, setCategories] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize] = useState(10)
  const [keyword, setKeyword] = useState('')
  const [filterCatId, setFilterCatId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null) // null=新增 | book=编辑
  const [form, setForm] = useState({ ...emptyForm })

  const fetchBooks = useCallback(async () => {
    setLoading(true)
    try {
      const params = { pageNum, pageSize }
      if (keyword.trim()) params.keyword = keyword.trim()
      if (filterCatId) params.categoryId = filterCatId
      const res = await bookApi.page(params)
      if (res.code === 200) { setBooks(res.data.records || []); setTotal(res.data.total || 0) }
      else setError(res.message)
    } catch (e) { setError(e.message || '加载失败') }
    finally { setLoading(false) }
  }, [pageNum, pageSize, keyword, filterCatId])

  const fetchCategories = async () => {
    try {
      const res = await categoryApi.list()
      if (res.code === 200) setCategories(res.data)
    } catch (e) { /* ignore */ }
  }

  useEffect(() => { fetchBooks() }, [fetchBooks])
  useEffect(() => { fetchCategories() }, [])

  const handleSearch = (e) => { e.preventDefault(); setPageNum(1); fetchBooks() }

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm })
    setShowModal(true)
  }

  const openEdit = (book) => {
    setEditing(book)
    setForm({
      title: book.title || '', author: book.author || '', isbn: book.isbn || '',
      publisher: book.publisher || '', price: book.price || '', stock: book.stock || '',
      categoryId: book.categoryId || '', coverUrl: book.coverUrl || '', description: book.description || ''
    })
    setShowModal(true)
  }

  const closeModal = () => { setShowModal(false); setError('') }

  const handleSave = async () => {
    if (!form.title.trim()) { setError('书名不能为空'); return }
    const data = {
      ...form,
      price: form.price ? Number(form.price) : null,
      stock: form.stock ? Number(form.stock) : 0,
      categoryId: form.categoryId ? Number(form.categoryId) : null
    }
    try {
      if (!editing) {
        const res = await bookApi.create(data)
        if (res.code !== 200) { setError(res.message); return }
      } else {
        const res = await bookApi.update(editing.id, data)
        if (res.code !== 200) { setError(res.message); return }
      }
      closeModal()
      fetchBooks()
    } catch (e) { setError(e.message || '保存失败') }
  }

  const handleDelete = async (id, title) => {
    if (!confirm(`确定删除图书「${title}」吗？`)) return
    try {
      const res = await bookApi.delete(id)
      if (res.code === 200) fetchBooks()
      else setError(res.message)
    } catch (e) { setError(e.message || '删除失败') }
  }

  const catName = (id) => categories.find(c => c.id === id)?.name || '-'
  const totalPages = Math.ceil(total / pageSize)

  return (
    <div className="page-wide">
      <div className="page-header">
        <h2>图书管理</h2>
        <button className="btn" onClick={openCreate}>+ 新增图书</button>
      </div>

      {/* 搜索栏 */}
      <form className="search-bar" onSubmit={handleSearch}>
        <input type="text" placeholder="搜索书名/作者/ISBN" value={keyword}
          onChange={e => setKeyword(e.target.value)} style={{ flex: 1 }} />
        <select value={filterCatId} onChange={e => { setFilterCatId(e.target.value); setPageNum(1) }}>
          <option value="">全部分类</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <button type="submit" className="btn">搜索</button>
      </form>

      {/* 弹框表单 */}
      {showModal && (
        <Modal title={editing ? '编辑图书' : '新增图书'} onClose={closeModal}>
          {error && <div className="error-msg">{error}</div>}
          <div className="form-row">
            <div className="form-group">
              <label>书名 *</label>
              <input value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} autoFocus />
            </div>
            <div className="form-group">
              <label>作者</label>
              <input value={form.author} onChange={e => setForm({ ...form, author: e.target.value })} />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>ISBN</label>
              <input value={form.isbn} onChange={e => setForm({ ...form, isbn: e.target.value })} />
            </div>
            <div className="form-group">
              <label>出版社</label>
              <input value={form.publisher} onChange={e => setForm({ ...form, publisher: e.target.value })} />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>价格</label>
              <input type="number" step="0.01" value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} />
            </div>
            <div className="form-group">
              <label>库存</label>
              <input type="number" value={form.stock} onChange={e => setForm({ ...form, stock: e.target.value })} />
            </div>
            <div className="form-group">
              <label>分类</label>
              <select value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })}>
                <option value="">请选择</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          </div>
          <div className="form-group">
            <label>封面URL</label>
            <input value={form.coverUrl} onChange={e => setForm({ ...form, coverUrl: e.target.value })} placeholder="选填" />
          </div>
          <div className="form-group">
            <label>简介</label>
            <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} placeholder="选填" />
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 20 }}>
            <button className="btn btn-outline" onClick={closeModal}>取消</button>
            <button className="btn" onClick={handleSave}>保存</button>
          </div>
        </Modal>
      )}

      {/* 图书列表 */}
      {loading ? <div className="card" style={{ textAlign: 'center' }}>加载中...</div> : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>ID</th><th>书名</th><th>作者</th><th>ISBN</th><th>价格</th><th>库存</th><th>分类</th><th>操作</th></tr>
            </thead>
            <tbody>
              {books.length === 0 ? (
                <tr><td colSpan={8} style={{ textAlign: 'center', color: '#999' }}>暂无数据</td></tr>
              ) : books.map(book => (
                <tr key={book.id}>
                  <td>{book.id}</td>
                  <td><strong>{book.title}</strong></td>
                  <td>{book.author || '-'}</td>
                  <td style={{ fontSize: 12, color: '#888' }}>{book.isbn || '-'}</td>
                  <td>{book.price ? `¥${book.price}` : '-'}</td>
                  <td>{book.stock}</td>
                  <td><span className="tag tag-role">{catName(book.categoryId)}</span></td>
                  <td>
                    <button className="btn-link" onClick={() => openEdit(book)}>编辑</button>
                    <button className="btn-link btn-danger" onClick={() => handleDelete(book.id, book.title)}>删除</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 分页 */}
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
