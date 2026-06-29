import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { auth } from '../api';

export default function OAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  useEffect(() => {
    const code = searchParams.get('code');
    if (!code) {
      setError('未收到授权码');
      return;
    }

    auth.loginWithGithub(code)
      .then(() => {
        // 跳回登录前正在看的页面（评论区所在文章页）
        const redirectTo = sessionStorage.getItem('eblog_redirect_after_login') || '/';
        sessionStorage.removeItem('eblog_redirect_after_login');
        navigate(redirectTo, { replace: true });
      })
      .catch(err => {
        setError(err.message || 'GitHub 登录失败');
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── 加载中 ──
  if (!error) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: 'calc(100vh - 56px)',
        padding: '24px',
        gap: '20px',
      }}>
        <div style={{
          width: 32,
          height: 32,
          border: '3px solid var(--border)',
          borderTopColor: 'var(--accent)',
          borderRadius: '50%',
          animation: 'oc-spin 0.7s linear infinite',
        }} />
        <style>{`@keyframes oc-spin{to{transform:rotate(360deg)}}`}</style>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem', margin: 0 }}>
          GitHub 登录中...
        </p>
      </div>
    );
  }

  // ── 错误状态 ──
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 'calc(100vh - 56px)',
      padding: '24px',
    }}>
      <div style={{
        background: 'var(--bg-card)',
        border: '1px solid var(--border)',
        borderRadius: '12px',
        padding: '32px',
        maxWidth: 400,
        width: '100%',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}>
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="1.5" style={{ margin: '0 auto' }}>
          <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
        <h2 style={{ fontSize: '1.1rem', margin: 0, color: 'var(--text-primary)' }}>GitHub 登录失败</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', margin: 0, lineHeight: 1.5 }}>{error}</p>
        <button onClick={() => navigate(-1)}
          style={{
            padding: '10px 24px',
            background: 'var(--accent)',
            color: '#fff',
            border: 'none',
            borderRadius: '8px',
            fontSize: '0.9rem',
            fontFamily: 'inherit',
            cursor: 'pointer',
            marginTop: 8,
          }}>
          返回
        </button>
      </div>
    </div>
  );
}