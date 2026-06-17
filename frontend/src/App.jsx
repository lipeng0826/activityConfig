import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Me from './pages/Me.jsx'
import Books from './pages/Books.jsx'
import Categories from './pages/Categories.jsx'
import Borrows from './pages/Borrows.jsx'
import Users from './pages/Users.jsx'
import Logs from './pages/Logs.jsx'

function Navbar() {
  const navigate = useNavigate()
  const token = localStorage.getItem('token')

  const handleLogout = () => {
    localStorage.removeItem('token')
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <div className="nav-brand">📚 图书订单系统</div>
      <div className="nav-links">
        {token ? (
          <>
            <Link to="/books">图书管理</Link>
            <Link to="/categories">分类管理</Link>
            <Link to="/borrows">我的借阅</Link>
            <Link to="/users">用户管理</Link>
            <Link to="/logs">操作日志</Link>
            <Link to="/me">个人中心</Link>
            <button onClick={handleLogout} className="btn btn-sm">退出</button>
          </>
        ) : (
          <>
            <Link to="/login">登录</Link>
            <Link to="/register">注册</Link>
          </>
        )}
      </div>
    </nav>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <div className="container">
        <Routes>
          <Route path="/" element={
            <div className="hero">
              <h1>📚 图书订单管理系统</h1>
              <p>基于 Spring Boot 3 + React 构建，支持 RBAC 权限管理</p>
              <div className="hero-actions">
                <Link to="/login" className="btn">登录</Link>
                <Link to="/register" className="btn btn-outline">注册</Link>
              </div>
            </div>
          } />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/me" element={<Me />} />
          <Route path="/books" element={<Books />} />
          <Route path="/categories" element={<Categories />} />
          <Route path="/borrows" element={<Borrows />} />
          <Route path="/users" element={<Users />} />
          <Route path="/logs" element={<Logs />} />
        </Routes>
      </div>
    </BrowserRouter>
  )
}
