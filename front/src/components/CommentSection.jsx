import { useState, useEffect } from 'react';
import api, { auth } from '../api';
import './CommentSection.css';

function CommentItem({ comment, articleId, onDelete, onReply }) {
  const [showReplyForm, setShowReplyForm] = useState(false);
  // avatarUrl is not returned by backend yet — use first char fallback

  return (
    <li className="comment-item">
      <div className="comment-meta">
        <span className="comment-author">
          <span className="comment-author-avatar">{comment.author?.charAt(0) || '?'}</span>
          {comment.author}
          {comment.parentId && <span className="comment-badge">回复</span>}
        </span>
        <span className="comment-date">
          {comment.createdAt
            ? new Date(comment.createdAt).toLocaleDateString('zh-CN', {
                year: 'numeric', month: 'long', day: 'numeric',
                hour: '2-digit', minute: '2-digit',
              })
            : ''}
        </span>
      </div>
      <p className="comment-body">{comment.content}</p>
      <div className="comment-actions">
        <button className="comment-reply-btn" onClick={() => setShowReplyForm(!showReplyForm)}>
          {showReplyForm ? '取消回复' : '回复'}
        </button>
        {auth.isAuthed() && (
          <button className="comment-delete" onClick={() => onDelete(comment.id)}>
            删除
          </button>
        )}
      </div>

      {showReplyForm && (
        <div className="comment-reply-form">
          <textarea
            className="comment-input-content comment-reply-input"
            placeholder={`回复 ${comment.author}...`}
            rows={2}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const textarea = e.target;
                if (textarea.value.trim()) {
                  onReply(comment.id, textarea.value.trim());
                  textarea.value = '';
                  setShowReplyForm(false);
                }
              }
            }}
          />
        </div>
      )}

      {/* Nested replies */}
      {comment.replies && comment.replies.length > 0 && (
        <ul className="comment-replies">
          {comment.replies.map(reply => (
            <CommentItem key={reply.id} comment={reply} articleId={articleId} onDelete={onDelete} onReply={onReply} />
          ))}
        </ul>
      )}
    </li>
  );
}

export default function CommentSection({ articleId }) {
  const [comments, setComments] = useState([]);
  const [author, setAuthor] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const isLoggedIn = auth.isAuthed();

  // Pre-fill author from GitHub identity if logged in
  useEffect(() => {
    if (isLoggedIn) {
      const nickname = auth.getNickname();
      if (nickname) setAuthor(nickname);
    }
  }, [isLoggedIn]);

  const fetchComments = () => {
    setLoading(true);
    api.getComments(articleId)
      .then(data => setComments(Array.isArray(data) ? data : []))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchComments();
  }, [articleId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!author.trim() || !content.trim()) return;

    setSubmitting(true);
    setError('');
    try {
      await api.createComment({
        articleId,
        author: author.trim(),
        content: content.trim(),
      });
      setAuthor('');
      setContent('');
      fetchComments(); // Re-fetch to get updated tree
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReply = async (parentId, replyContent) => {
    if (!author.trim()) {
      setError('请先填写昵称');
      return;
    }
    try {
      await api.createComment({
        articleId,
        parentId,
        author: author.trim(),
        content: replyContent,
      });
      fetchComments();
    } catch (err) {
      setError(err.message);
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

  const totalComments = comments.reduce((sum, c) => sum + 1 + (c.replies?.length || 0), 0);

  return (
    <div className="comment-section">
      <div className="comment-header-area">
        <h2 className="comment-title">评论</h2>
        {!loading && <span className="comment-count">{totalComments}</span>}
      </div>

      {error && <div className="comment-error">{error}</div>}

      {/* 评论列表 */}
      {loading ? (
        <div className="comment-status">加载中...</div>
      ) : comments.length === 0 ? (
        <div className="comment-status">暂无评论，来说点什么吧</div>
      ) : (
        <ul className="comment-list">
          {comments.map(c => (
            <CommentItem key={c.id} comment={c} articleId={articleId} onDelete={handleDelete} onReply={handleReply} />
          ))}
        </ul>
      )}

      {/* 发表评论 */}
      <form className="comment-form" onSubmit={handleSubmit}>
        <h3 className="comment-form-title">发表评论</h3>
        {isLoggedIn ? (
          <div className="comment-loggedin-info">
            <span className="comment-author-avatar">{author.charAt(0)}</span>
            <span>{author}</span>
          </div>
        ) : (
          <input
            type="text"
            className="comment-input-author"
            placeholder="你的昵称"
            value={author}
            onChange={e => setAuthor(e.target.value)}
            maxLength={30}
            required
          />
        )}
        <textarea
          className="comment-input-content"
          placeholder="写下你的评论..."
          value={content}
          onChange={e => setContent(e.target.value)}
          rows={4}
          maxLength={1000}
          required
        />
        <div className="comment-form-footer">
          <span className="comment-hint">{content.length}/1000</span>
          <button
            type="submit"
            className="comment-submit"
            disabled={submitting || !author.trim() || !content.trim()}
          >
            {submitting ? '提交中...' : '发表评论'}
          </button>
        </div>
      </form>
    </div>
  );
}