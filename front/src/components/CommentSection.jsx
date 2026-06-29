import { useState, useEffect, useCallback } from 'react';
import api, { auth } from '../api';
import './CommentSection.css';

const GITHUB_CLIENT_ID = 'Ov23liVuTKWsgzFjZpbN';

// ─── 相对时间 ───
function relativeTime(dateStr) {
  if (!dateStr) return '';
  const now = Date.now();
  const then = new Date(dateStr).getTime();
  const diff = now - then;
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return '刚刚';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}天前`;
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'numeric', day: 'numeric',
  });
}

// ─── 头像 ───
function Avatar({ url, name, size = 32 }) {
  const fallback = name?.charAt(0)?.toUpperCase() || '?';
  if (url) {
    return (
      <img
        className="c-avatar"
        src={url}
        alt={name}
        width={size}
        height={size}
        onError={(e) => {
          e.target.style.display = 'none';
          if (e.target.nextSibling) e.target.nextSibling.style.display = 'flex';
        }}
      />
    );
  }
  return (
    <span
      className="c-avatar c-avatar--fallback"
      style={{ width: size, height: size, fontSize: size * 0.42 }}
    >
      {fallback}
    </span>
  );
}

// ─── GitHub 登录按钮 ───
function GithubLoginButton({ label = 'GitHub 登录' }) {
  const handleClick = () => {
    // 记住当前页面，回调后跳回来
    sessionStorage.setItem('eblog_redirect_after_login', window.location.pathname);
    const redirectUri = `${window.location.origin}/oauth/callback`;
    const url = `https://github.com/login/oauth/authorize?client_id=${GITHUB_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=read:user,user:email`;
    window.location.href = url;
  };

  return (
    <button className="c-github-btn" onClick={handleClick}>
      <svg width="18" height="18" viewBox="0 0 16 16" fill="currentColor">
        <path fillRule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
      </svg>
      {label}
    </button>
  );
}

// ─── 单条评论 ───
function CommentItem({ comment, articleId, onDelete, onReply }) {
  const isLoggedIn = auth.isAuthed();
  const isOwner = isLoggedIn && auth.getUserId() === String(comment.userId);
  const canDelete = isLoggedIn && (auth.isAdmin() || isOwner);
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [replyText, setReplyText] = useState('');

  const handleReplySubmit = () => {
    if (!replyText.trim() || !onReply) return;
    onReply(comment.id, replyText.trim());
    setReplyText('');
    setShowReplyForm(false);
  };

  return (
    <li className="c-item">
      <div className="c-item-main">
        <Avatar url={comment.avatarUrl} name={comment.author} />
        <div className="c-body">
          <div className="c-meta">
            <span className="c-author">
              {comment.author}
              {comment.replyToName && <span className="c-reply-to">回复 @{comment.replyToName}</span>}
              {comment.userId && <span className="c-badge-github" title="GitHub 用户">GitHub</span>}
            </span>
            <span className="c-time">{relativeTime(comment.createdAt)}</span>
          </div>
          <p className="c-content">{comment.content}</p>
          <div className="c-actions">
            {isLoggedIn ? (
              <button
                className="c-action-btn"
                onClick={() => setShowReplyForm(!showReplyForm)}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                {showReplyForm ? '取消' : '回复'}
              </button>
            ) : (
              <span className="c-action-hint">登录后可回复</span>
            )}
            {canDelete && (
              <button className="c-action-btn c-action-btn--delete" onClick={() => onDelete(comment.id)}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                删除
              </button>
            )}
          </div>

          {/* 回复表单 (仅登录用户) */}
          {showReplyForm && isLoggedIn && (
            <div className="c-reply-form">
              <textarea
                className="c-input c-input--reply"
                placeholder={`回复 ${comment.author}...`}
                value={replyText}
                onChange={e => setReplyText(e.target.value)}
                rows={2}
                maxLength={1000}
              />
              <div className="c-reply-form-actions">
                <span className="c-hint">{replyText.length}/1000</span>
                <button
                  className="c-btn c-btn--sm"
                  disabled={!replyText.trim()}
                  onClick={handleReplySubmit}
                >
                  回复
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 嵌套回复 */}
      {comment.replies && comment.replies.length > 0 && (
        <ul className="c-replies">
          {comment.replies.map(reply => (
            <CommentItem key={reply.id} comment={reply} articleId={articleId} onDelete={onDelete} onReply={onReply} />
          ))}
        </ul>
      )}
    </li>
  );
}

// ─── 评论区主组件 ───
export default function CommentSection({ articleId }) {
  const [comments, setComments] = useState([]);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const isLoggedIn = auth.isAuthed();
  const nickname = auth.getNickname();

  const fetchComments = useCallback(() => {
    setLoading(true);
    api.getComments(articleId)
      .then(data => setComments(Array.isArray(data) ? data : []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [articleId]);

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!content.trim() || !isLoggedIn) return;

    setSubmitting(true);
    setError('');
    try {
      await api.createComment({
        articleId,
        author: nickname,
        content: content.trim(),
        avatarUrl: auth.getAvatar(),
      });
      setContent('');
      fetchComments();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('确定删除这条评论？')) return;
    try {
      await api.deleteComment(id);
      setComments(prev => prev.filter(c => c.id !== id));
    } catch (err) {
      setError(err.message);
    }
  };

  const handleReply = async (parentId, replyContent) => {
    try {
      await api.createComment({
        articleId,
        parentId,
        author: nickname,
        content: replyContent,
        avatarUrl: auth.getAvatar(),
      });
      fetchComments();
    } catch (err) {
      setError(err.message);
    }
  };

  const totalComments = comments.reduce((sum, c) => sum + 1 + (c.replies?.length || 0), 0);

  return (
    <div className="c-section">
      <div className="c-header">
        <h2 className="c-title">评论</h2>
        {!loading && <span className="c-count">{totalComments} 条评论</span>}
      </div>

      {error && <div className="c-error">{error}</div>}

      {/* 评论列表 */}
      {loading ? (
        <div className="c-empty">
          <div className="c-spinner" />
          加载中...
        </div>
      ) : comments.length === 0 ? (
        <div className="c-empty">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--border)" strokeWidth="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <p>暂无评论</p>
        </div>
      ) : (
        <ul className="c-list">
          {comments.map(c => (
            <CommentItem key={c.id} comment={c} articleId={articleId} onDelete={handleDelete} onReply={handleReply} />
          ))}
        </ul>
      )}

      {/* 发表评论 / 登录提示 */}
      {isLoggedIn ? (
        <form className="c-form" onSubmit={handleSubmit}>
          <h3 className="c-form-title">发表评论</h3>
          <div className="c-loggedin">
            <Avatar url={auth.getAvatar()} name={nickname} size={36} />
            <div className="c-loggedin-info">
              <span className="c-loggedin-name">{nickname}</span>
              <span className="c-loggedin-tag">
                <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor"><path fillRule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>
                GitHub
              </span>
            </div>
          </div>
          <textarea
            className="c-input c-input--content"
            placeholder="写下你的评论..."
            value={content}
            onChange={e => setContent(e.target.value)}
            rows={3}
            maxLength={1000}
            required
          />
          <div className="c-form-footer">
            <span className="c-hint">{content.length}/1000</span>
            <button
              type="submit"
              className="c-btn c-btn--primary"
              disabled={submitting || !content.trim()}
            >
              {submitting ? (
                <><span className="c-spinner c-spinner--btn" /> 提交中...</>
              ) : (
                '发表评论'
              )}
            </button>
          </div>
        </form>
      ) : (
        <div className="c-login-prompt">
          <p>登录后即可发表评论</p>
          <GithubLoginButton />
        </div>
      )}
    </div>
  );
}
