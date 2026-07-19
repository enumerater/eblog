import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { auth } from '../api';
import './Navbar.css';

function getInitialTheme() {
  const saved = localStorage.getItem('eblog_theme');
  if (saved) return saved;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export default function Navbar() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const isAuthed = auth.isAuthed();
  const [theme, setTheme] = useState(getInitialTheme);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('eblog_theme', theme);
  }, [theme]);

  const isActive = (path) => pathname === path;

  const handleLogout = async () => {
    await auth.logout();
    navigate('/');
  };

  const toggleTheme = () => {
    setTheme(t => t === 'light' ? 'dark' : 'light');
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">enumerate</Link>
        <div className="navbar-links">
          <Link to="/" className={isActive('/') ? 'active' : ''}>首页</Link>
          {isAuthed ? (
            <>
              <Link to="/admin" className={isActive('/admin') ? 'active' : ''}>管理</Link>
              <Link to="/diary" className={isActive('/diary') ? 'active' : ''}>日记</Link>
              <Link to="/editor" className={isActive('/editor') ? 'active' : ''}>写作</Link>
              <button className="navbar-logout" onClick={handleLogout}>退出</button>
            </>
          ) : (
            <Link to="/login" className={isActive('/login') ? 'active' : ''}>登录</Link>
          )}
        </div>
        <button className="theme-toggle" onClick={toggleTheme} title={theme === 'light' ? '切换深色模式' : '切换浅色模式'}>
          {theme === 'light' ? '☽' : '☀'}
        </button>
      </div>
    </nav>
  );
}
