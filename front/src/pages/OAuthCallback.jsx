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
        navigate('/', { replace: true });
      })
      .catch(err => {
        setError(err.message || 'GitHub 登录失败');
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="login-page">
      <div className="login-box">
        <h1>GitHub 登录</h1>
        {error ? (
          <>
            <p className="login-error">{error}</p>
            <button onClick={() => navigate('/login')} className="btn" style={{ marginTop: 16 }}>
              返回登录
            </button>
          </>
        ) : (
          <p>正在登录...</p>
        )}
      </div>
    </div>
  );
}