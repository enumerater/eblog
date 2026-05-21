import { useState, useEffect } from 'react';
import api, { auth } from '../api';
import './CommentSection.css';

export default function CommentSection({ articleId }) {
  const [comments, setComments] = useState([]);
  const [author, setAuthor] = useState('');
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    api.getComments(articleId)
      .then(data => setComments(data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [articleId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!author.trim() || !content.trim()) return;

    setSubmitting(true);
    setError('');
    try {
      const saved = await api.createComment(articleId, {
        author: author.trim(),
        content: content.trim(),
      });
      setComments(prev => [...prev, saved]);
      setAuthor('');
      setContent('');
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('确定删除这条评论？')) return;
    try {
      await api.deleteComment(articleId, id);
      setComments(prev => prev.filter(c => c.id !== id));
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="comment-section">
      <div className="comment-header-area">
        <h2 className="comment-title">评论</h2>
        {!loading && <span className="comment-count">{comments.length}</span>}
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
            <li key={c.id} className="comment-item">
              <div className="comment-meta">
                <span className="comment-author">
                  <span className="comment-author-avatar">
                    {c.author.charAt(0)}
                  </span>
                  {c.author}
                </span>
                <span className="comment-date">
                  {new Date(c.createdAt).toLocaleDateString('zh-CN', {
                    year: 'numeric', month: 'long', day: 'numeric',
                    hour: '2-digit', minute: '2-digit',
                  })}
                </span>
              </div>
              <p className="comment-body">{c.content}</p>
              {auth.isAuthed() && (
                <div className="comment-actions">
                  <button
                    className="comment-delete"
                    onClick={() => handleDelete(c.id)}
                  >
                    删除
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {/* 发表评论 */}
      <form className="comment-form" onSubmit={handleSubmit}>
        <h3 className="comment-form-title">发表评论</h3>
        <input
          type="text"
          className="comment-input-author"
          placeholder="你的昵称"
          value={author}
          onChange={e => setAuthor(e.target.value)}
          maxLength={30}
          required
        />
        <textarea
          className="comment-input-content"
          placeholder="写下你的评论..."
          value={content}
          onChange={e => setContent(e.target.value)}
          rows={4}
          maxLength={500}
          required
        />
        <div className="comment-form-footer">
          <span className="comment-hint">{content.length}/500</span>
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