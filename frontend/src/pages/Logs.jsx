import { useState, useEffect, useCallback } from 'react'
import { logApi } from '../api/index.js'

const MODULES = ['', 'book', 'category', 'borrow', 'user']

export default function Logs() {
  const [logs, setLogs] = useState([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [filterModule, setFilterModule] = useState('')
  const [loading, setLoading] = useState(true)

  const fetchLogs = useCallback(async () => {
    setLoading(true)
    try {
      const params = { pageNum, pageSize: 20 }
      if (filterModule) params.module = filterModule
      const res = await logApi.page(params)
      if (res.code === 200) { setLogs(res.data.records || []); setTotal(res.data.total || 0) }
    } catch (e) { /* ignore */ }
    finally { setLoading(false) }
  }, [pageNum, filterModule])

  useEffect(() => { fetchLogs() }, [fetchLogs])

  const totalPages = Math.ceil(total / 20)
  const fmt = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-'

  return (
    <div className="page-wide">
      <div className="page-header"><h2>操作日志</h2></div>

      <div className="search-bar" style={{ marginBottom: 16 }}>
        <select value={filterModule} onChange={e => { setFilterModule(e.target.value); setPageNum(1) }}>
          {MODULES.map(m => <option key={m} value={m}>{m || '全部模块'}</option>)}
        </select>
      </div>

      {loading ? <div className="card" style={{ textAlign: 'center' }}>加载中...</div> : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>时间</th><th>用户</th><th>模块</th><th>操作</th><th>详情</th><th>IP</th></tr>
            </thead>
            <tbody>
              {logs.length === 0 ? (
                <tr><td colSpan={6} style={{ textAlign: 'center', color: '#999' }}>暂无日志</td></tr>
              ) : logs.map(log => (
                <tr key={log.id}>
                  <td style={{ fontSize: 12, whiteSpace: 'nowrap' }}>{fmt(log.createTime)}</td>
                  <td><strong>{log.username || '-'}</strong></td>
                  <td><span className="tag">{log.module}</span></td>
                  <td><span className="tag tag-role">{log.operation}</span></td>
                  <td style={{ fontSize: 12, color: '#888', maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis' }}>{log.detail || '-'}</td>
                  <td style={{ fontSize: 12, color: '#888' }}>{log.ip || '-'}</td>
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
