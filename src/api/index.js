const BASE_URL = 'http://localhost:3001/api';

// const BASE_URL = '/api';

function getToken() {
  return localStorage.getItem('eblog_token');
}

async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const config = { headers, ...options };
  const res = await fetch(`${BASE_URL}${url}`, config);
  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem('eblog_token');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || `请求失败: ${res.status}`);
  }
  return res.json();
}

export const auth = {
  login(password) {
    return request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ password }),
    });
  },
  isAuthed() {
    return !!getToken();
  },
  setToken(token) {
    localStorage.setItem('eblog_token', token);
  },
  logout() {
    localStorage.removeItem('eblog_token');
  },
};

export const api = {
  getArticles() {
    return request('/articles');
  },
  searchArticles(params) {
    const qs = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v))
    ).toString();
    return request(`/articles${qs ? '?' + qs : ''}`);
  },
  getArticle(id) {
    return request(`/articles/${id}`);
  },
  createArticle(data) {
    return request('/articles', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateArticle(id, data) {
    return request(`/articles/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  deleteArticle(id) {
    return request(`/articles/${id}`, {
      method: 'DELETE',
    });
  },

  getDrafts() {
    return request('/drafts');
  },
  getDraft(id) {
    return request(`/drafts/${id}`);
  },
  createDraft(data) {
    return request('/drafts', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  updateDraft(id, data) {
    return request(`/drafts/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
  deleteDraft(id) {
    return request(`/drafts/${id}`, {
      method: 'DELETE',
    });
  },
  publishDraft(id, data = {}) {
    return request(`/drafts/${id}/publish`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  getComments(articleId) {
    return request(`/articles/${articleId}/comments`);
  },
  createComment(articleId, data) {
    return request(`/articles/${articleId}/comments`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
  deleteComment(articleId, id) {
    return request(`/articles/${articleId}/comments/${id}`, {
      method: 'DELETE',
    });
  },
};

export default api;
