import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth } from '../api';
import './Login.css';

const GITHUB_CLIENT_ID = 'Ov23liVuTKWsgzFjZpbN'; // ← 用户需填写自己的 GitHub OAuth App Client ID

export default function Login() {
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!password.trim()) { setError('请输入密码'); return; }

    setLoading(true);
    setError('');

    try {
      await auth.login(password);
      navigate('/');
    } catch (err) {
      setError(err.message || '密码错误');
    } finally {
      setLoading(false);
    }
  };

  const handleGithubLogin = () => {
    const clientId = GITHUB_CLIENT_ID;
    if (!clientId) {
      setError('GitHub OAuth 尚未配置 Client ID');
      return;
    }
    const redirectUri = `${window.location.origin}/oauth/callback`;
    const url = `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=read:user,user:email`;
    window.location.href = url;
  };

  return (
    <div className="login-page">
      <div className="login-box">
        <h1>登录</h1>

        {error && <div className="login-error">{error}</div>}

        {/* 用户登录 — GitHub */}
        <div className="login-section">
          <p className="login-section-title">用户登录</p>
          <p className="login-section-desc">使用 GitHub 账号登录，即可发表评论</p>
          <button className="github-login-btn" onClick={handleGithubLogin}>
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path fillRule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
            </svg>
            GitHub 登录
          </button>
        </div>

        <div className="login-divider">
          <span>或</span>
        </div>

        {/* 管理员登录 */}
        <div className="login-section">
          <p className="login-section-title">管理员登录</p>
          <form onSubmit={handleSubmit}>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="管理密码"
              autoFocus
            />
            <button type="submit" disabled={loading}>
              {loading ? '验证中...' : '登录'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
