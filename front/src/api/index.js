const BASE_URL = '/api';

// ──────────────────────────────────────────────
// Token Management
// ──────────────────────────────────────────────

function getAccessToken() {
  return localStorage.getItem('eblog_token');
}

function getRefreshToken() {
  return localStorage.getItem('eblog_refresh_token');
}

function setTokens(accessToken, refreshToken) {
  localStorage.setItem('eblog_token', accessToken);
  if (refreshToken) {
    localStorage.setItem('eblog_refresh_token', refreshToken);
  }
}

function clearTokens() {
  localStorage.removeItem('eblog_token');
  localStorage.removeItem('eblog_refresh_token');
  localStorage.removeItem('eblog_user_id');
}

/** Decode JWT payload (no verification — client-side only for reading claims). */
function decodeJwtPayload(token) {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    return JSON.parse(atob(parts[1]));
  } catch {
    return null;
  }
}

function isAuthEndpoint(url) {
  return url.startsWith('/auth/');
}

let refreshPromise = null;

/**
 * Attempt to refresh the access token using the stored refresh token.
 * Uses a promise lock so concurrent 401s share a single refresh attempt.
 * Returns the new access token, or null if refresh failed.
 */
async function attemptTokenRefresh() {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    const token = getRefreshToken();
    if (!token) return null;

    try {
      const res = await fetch(`${BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: token }),
      });

      if (!res.ok) return null;

      const json = await res.json().catch(() => null);
      if (!json) return null;

      const data = (json.code !== undefined && json.code === 200) ? json.data : json;
      if (!data || !data.accessToken) return null;

      setTokens(data.accessToken, data.refreshToken || token);

      // Update stored userId from new token
      const payload = decodeJwtPayload(data.accessToken);
      if (payload && payload.sub) {
        localStorage.setItem('eblog_user_id', payload.sub);
      }

      return data.accessToken;
    } catch {
      return null;
    }
  })();

  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

/**
 * Unwrap Result<T> response wrapper if present.
 *
 * Backend controllers follow two patterns:
 *  - AuthController wraps everything in Result<T> → { code, message, data, timestamp }
 *  - Article/Comment/Draft/Upload return raw entities/objects
 */
function unwrapResponse(json) {
  if (json && typeof json === 'object' && 'code' in json) {
    if (json.code === 200) return json.data;
    throw new Error(json.message || '请求失败');
  }
  return json;
}

// ──────────────────────────────────────────────
// Core request helper
// ──────────────────────────────────────────────

async function request(url, options = {}) {
  const headers = {};
  // Let browser set multipart boundary for FormData
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const token = getAccessToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const mergedHeaders = { ...headers, ...options.headers };
  const config = { ...options, headers: mergedHeaders };

  let res = await fetch(`${BASE_URL}${url}`, config);

  // ── 401 handling: attempt token refresh once ──
  if (res.status === 401 && !isAuthEndpoint(url)) {
    const newToken = await attemptTokenRefresh();
    if (newToken) {
      // Retry with the new token
      mergedHeaders['Authorization'] = `Bearer ${newToken}`;
      config.headers = mergedHeaders;
      res = await fetch(`${BASE_URL}${url}`, config);
    } else {
      clearTokens();
      redirectToLogin();
      throw new Error('登录已过期，请重新登录');
    }
  }

  // ── Error responses ──
  if (!res.ok) {
    if (res.status === 401) {
      clearTokens();
      redirectToLogin();
    }
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || `请求失败: ${res.status}`);
  }

  // ── Success responses ──
  const json = await res.json();
  return unwrapResponse(json);
}

// ──────────────────────────────────────────────
// Auth Module
// ──────────────────────────────────────────────

export const auth = {
  async login(password) {
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: '密码错误' }));
      throw new Error(err.message || '密码错误');
    }

    const json = await res.json();

    // AuthController always returns Result<T>
    if (json.code !== 200) {
      throw new Error(json.message || '登录失败');
    }

    const data = json.data || json;

    if (data.accessToken) {
      setTokens(data.accessToken, data.refreshToken);

      // Store userId for logout
      const payload = decodeJwtPayload(data.accessToken);
      if (payload && payload.sub) {
        localStorage.setItem('eblog_user_id', payload.sub);
      }

      return data;
    }

    throw new Error('登录响应异常');
  },

  isAuthed() {
    return !!getAccessToken();
  },

  getUserId() {
    return localStorage.getItem('eblog_user_id');
  },

  async logout() {
    const userId = this.getUserId();
    try {
      if (userId) {
        await fetch(`${BASE_URL}/auth/logout`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${getAccessToken()}`,
            'X-User-Id': userId,
          },
        });
      }
    } catch {
      // Best-effort — still clear local tokens even if API call fails
    }
    clearTokens();
  },
};

// ──────────────────────────────────────────────
// API Module
// ──────────────────────────────────────────────

export const api = {
  // ── Articles (article-service) ──

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

  // ── Drafts ──

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

  // ── Comments (comment-service) ──

  getComments(articleId) {
    return request(`/comments/article/${articleId}`);
  },

  createComment(data) {
    return request('/comments', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  deleteComment(id) {
    return request(`/comments/${id}`, {
      method: 'DELETE',
    });
  },

  getCommentCount(articleId) {
    return request(`/comments/count/${articleId}`);
  },

  approveComment(id) {
    return request(`/comments/admin/${id}/approve`, { method: 'PUT' });
  },

  rejectComment(id) {
    return request(`/comments/admin/${id}/reject`, { method: 'PUT' });
  },

  // ── Upload (article-service, legacy) ──

  uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request('/upload/image', {
      method: 'POST',
      body: formData,
      headers: {},
    });
  },

  // ══════════════════════════════════════════════
  // New Services
  // ══════════════════════════════════════════════

  // ── Query Service (article-query-service) ──

  getArticlesPage(page = 1, size = 10, tag) {
    const params = { page, size };
    if (tag) params.tag = tag;
    const qs = new URLSearchParams(params).toString();
    return request(`/articles-query?${qs}`);
  },

  getArticleDetail(id) {
    return request(`/articles-query/${id}`);
  },

  getHotArticles(limit = 10) {
    return request(`/articles-query/hot?limit=${limit}`);
  },

  getTagCloud() {
    return request('/articles-query/tags');
  },

  // ── Search Service ──

  searchFullText({ keyword, tag, page = 1, size = 10 } = {}) {
    const params = { page, size };
    if (keyword) params.keyword = keyword;
    if (tag) params.tag = tag;
    const qs = new URLSearchParams(params).toString();
    return request(`/search/articles?${qs}`);
  },

  getSearchSuggestions(keyword) {
    return request(`/search/suggestions?keyword=${encodeURIComponent(keyword)}`);
  },

  getHotSearches(limit = 10) {
    return request(`/search/hot?limit=${limit}`);
  },

  // ── Intelligence Service ──

  getArticleSummary(id) {
    return request(`/intelligence/articles/${id}/summary`);
  },

  getArticleStats(id) {
    return request(`/intelligence/articles/${id}/stats`);
  },

  getRecommendations(articleId, limit = 5) {
    return request(`/intelligence/recommendations/${articleId}?limit=${limit}`);
  },

  suggestTags(content) {
    return request('/intelligence/tags/suggest', {
      method: 'POST',
      body: JSON.stringify({ content }),
    });
  },

  // ── Notification Service ──

  getNotifications(page = 1, size = 10) {
    return request(`/notifications?page=${page}&size=${size}`);
  },

  getUnreadCount() {
    return request('/notifications/unread-count');
  },

  markNotificationAsRead(id) {
    return request(`/notifications/${id}/read`, { method: 'PUT' });
  },

  markAllNotificationsAsRead() {
    return request('/notifications/read-all', { method: 'PUT' });
  },

  sendSystemNotification(data) {
    return request('/notifications/admin', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // ── File Service ──

  uploadFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request('/files/upload', {
      method: 'POST',
      body: formData,
      headers: {},
    });
  },

  getFileList(type, page = 1, size = 20) {
    const params = { page, size };
    if (type) params.type = type;
    const qs = new URLSearchParams(params).toString();
    return request(`/files?${qs}`);
  },

  getFileInfo(id) {
    return request(`/files/${id}`);
  },

  deleteFile(id) {
    return request(`/files/${id}`, { method: 'DELETE' });
  },
};

export default api;
