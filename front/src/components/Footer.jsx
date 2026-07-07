import { useState, useEffect, useRef } from 'react';
import api from '../api';
import './Footer.css';

export default function Footer() {
  const year = new Date().getFullYear();
  const [onlineCount, setOnlineCount] = useState(null);
  const sessionIdRef = useRef(null);
  const heartbeatTimerRef = useRef(null);

  // ── 心跳 + 在线人数 ──
  useEffect(() => {
    // 初始化 sessionId
    let sid = localStorage.getItem('eblog_session_id');
    if (!sid) {
      sid = 'anon:' + crypto.randomUUID().replace(/-/g, '');
      localStorage.setItem('eblog_session_id', sid);
    }
    sessionIdRef.current = sid;

    // 立即发送一次心跳
    api.heartbeat(sid).catch(() => {});

    // 每 60s 发送心跳并更新在线人数
    const heartbeat = async () => {
      try {
        await api.heartbeat(sid);
        const count = await api.getOnlineCount();
        setOnlineCount(count);
      } catch { /* ignore */ }
    };
    heartbeatTimerRef.current = setInterval(heartbeat, 60000);
    // 首次快速获取在线人数
    api.getOnlineCount().then(c => setOnlineCount(c)).catch(() => {});

    return () => {
      clearInterval(heartbeatTimerRef.current);
    };
  }, []);

  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div className="footer-left">
          <span className="footer-brand">enumerate</span>
          <span className="footer-sep">&middot;</span>
          <span className="footer-copy">&copy; {year}</span>
        </div>
        <div className="footer-right">
          {onlineCount !== null && (
            <span className="footer-online" title="实时在线人数">
              🟢 {onlineCount} 人在线
            </span>
          )}
          <span className="footer-sep">&middot;</span>
          <a href="https://github.com/enumerater" target="_blank" rel="noopener noreferrer">GitHub</a>
        </div>
      </div>
    </footer>
  );
}
