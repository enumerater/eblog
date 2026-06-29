import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect, useCallback, useRef } from 'react';
import api, { auth } from '../api';
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
  const [unreadCount, setUnreadCount] = useState(0);
  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [notifLoading, setNotifLoading] = useState(false);
  const navbarRef = useRef(null);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('eblog_theme', theme);
  }, [theme]);

  // Fetch unread count periodically
  const fetchUnread = useCallback(async () => {
    if (!isAuthed) return;
    try {
      const count = await api.getUnreadCount();
      setUnreadCount(count || 0);
    } catch {}
  }, [isAuthed]);

  useEffect(() => {
    fetchUnread();
    const interval = setInterval(fetchUnread, 30000); // every 30s
    return () => clearInterval(interval);
  }, [fetchUnread]);

  // Click outside to close notification dropdown
  useEffect(() => {
    const onClick = (e) => {
      if (navbarRef.current && !navbarRef.current.contains(e.target)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const openNotifications = () => {
    if (showNotifications) {
      setShowNotifications(false);
      return;
    }
    setShowNotifications(true);
    setNotifLoading(true);
    api.getNotifications(1, 5)
      .then(data => setNotifications(data || []))
      .catch(() => setNotifications([]))
      .finally(() => setNotifLoading(false));
  };

  const markAllRead = async () => {
    try {
      await api.markAllNotificationsAsRead();
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch {}
  };

  const markOneRead = async (id) => {
    try {
      await api.markNotificationAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch {}
  };

  const isActive = (path) => pathname === path;

  const handleLogout = async () => {
    await auth.logout();
    navigate('/');
  };

  const toggleTheme = () => {
    setTheme(t => t === 'light' ? 'dark' : 'light');
  };

  return (
    <nav className="navbar" ref={navbarRef}>
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">enumerate</Link>
        <div className="navbar-links">
          <Link to="/" className={isActive('/') ? 'active' : ''}>首页</Link>
          {isAuthed ? (
            <>
              <Link to="/admin" className={isActive('/admin') ? 'active' : ''}>管理</Link>
              <Link to="/editor" className={isActive('/editor') ? 'active' : ''}>写作</Link>
              <button className="notification-bell" onClick={openNotifications} title="通知">
                <span className="bell-icon">🔔</span>
                <span className="bell-label">通知</span>
                {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
              </button>
              <button className="navbar-logout" onClick={handleLogout}>退出</button>
            </>
          ) : (
            <Link to="/login" className={isActive('/login') ? 'active' : ''}>登录</Link>
          )}
        </div>
        {/* 通知下拉 — 放在 navbar-links 外面, 避免 overflow 裁剪 */}
        {isAuthed && showNotifications && (
          <div className="notification-dropdown">
            <div className="notification-dropdown-header">
              <span>通知</span>
              <button className="notif-mark-all" onClick={markAllRead}>全部已读</button>
            </div>
            {notifLoading ? (
              <div className="notification-item">加载中...</div>
            ) : notifications.length === 0 ? (
              <div className="notification-item">暂无通知</div>
            ) : (
              notifications.map(n => (
                <div key={n.id} className={`notification-item ${!n.isRead ? 'unread' : ''}`}
                     onClick={() => markOneRead(n.id)}>
                  <div className="notif-title">{n.title}</div>
                  <div className="notif-time">
                    {n.createdAt ? new Date(n.createdAt).toLocaleDateString('zh-CN') : ''}
                  </div>
                </div>
              ))
            )}
          </div>
        )}
        <button className="theme-toggle" onClick={toggleTheme} title={theme === 'light' ? '切换深色模式' : '切换浅色模式'}>
          {theme === 'light' ? '☽' : '☀'}
        </button>
      </div>
    </nav>
  );
}
