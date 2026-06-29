import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth } from '../api';
import './Login.css';

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

  return (
    <div className="login-page">
      <div className="login-box">
        <h1>管理员登录</h1>

        {error && <div className="login-error">{error}</div>}

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
  );
}