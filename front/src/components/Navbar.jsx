import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect, useCallback } from 'react';
import { auth, api } from '../api';
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

  const openNotifications = async () => {
    setShowNotifications(!showNotifications);
    if (!showNotifications) {
      setNotifLoading(true);
      try {
        const data = await api.getNotifications(1, 5);
        setNotifications(data || []);
      } catch {}
      setNotifLoading(false);
    }
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
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">enumerate</Link>
        <div className="navbar-links">
          <Link to="/" className={isActive('/') ? 'active' : ''}>首页</Link>
          {isAuthed ? (
            <>
              <Link to="/admin" className={isActive('/admin') ? 'active' : ''}>管理</Link>
              <Link to="/editor" className={isActive('/editor') ? 'active' : ''}>写作</Link>
              <div className="notification-bell-wrapper">
                <button className="notification-bell" onClick={openNotifications} title="通知">
                  🔔
                  {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
                </button>
                {showNotifications && (
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
              </div>
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
