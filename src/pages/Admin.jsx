import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import './Admin.css';

export default function Admin() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [drafts, setDrafts] = useState([]);
  const [draftsLoading, setDraftsLoading] = useState(true);
  const [showDrafts, setShowDrafts] = useState(false);

  const fetchArticles = () => {
    setLoading(true);
    api.getArticles()
      .then(data => setArticles(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  };

  const fetchDrafts = () => {
    setDraftsLoading(true);
    api.getDrafts()
      .then(data => setDrafts(data))
      .catch(() => {})
      .finally(() => setDraftsLoading(false));
  };

  useEffect(() => { fetchArticles(); fetchDrafts(); }, []);

  const handleDelete = async (id, title) => {
    if (!window.confirm(`确定要删除「${title}」吗？此操作不可撤销。`)) return;
    try {
      await api.deleteArticle(id);
      setArticles(articles.filter(a => a.id !== id));
    } catch (err) {
      alert('删除失败: ' + err.message);
    }
  };

  const handleDeleteDraft = async (id, title) => {
    if (!window.confirm(`确定要删除草稿「${title || '无标题'}」吗？`)) return;
    try {
      await api.deleteDraft(id);
      setDrafts(drafts.filter(d => d.id !== id));
    } catch (err) {
      alert('删除失败: ' + err.message);
    }
  };

  if (loading) return <div className="page-status">加载中...</div>;
  if (error) return <div className="page-status error">{error}</div>;

  return (
    <div className="admin-page">
      <div className="admin-header">
        <h1>文章管理</h1>
        <Link to="/editor" className="btn btn-edit">写新文章</Link>
      </div>

      {articles.length === 0 && <p className="empty">还没有文章</p>}

      <table className="admin-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>标签</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {articles.map(article => (
            <tr key={article.id}>
              <td>
                <Link to={`/article/${article.id}`}>{article.title}</Link>
              </td>
              <td>
                {article.tags?.length > 0
                  ? article.tags.map(t => <span key={t} className="tag">{t}</span>)
                  : <span className="td-empty">-</span>}
              </td>
              <td className="td-date">
                {new Date(article.createdAt).toLocaleDateString('zh-CN')}
              </td>
              <td className="td-actions">
                <Link to={`/editor?id=${article.id}`} className="btn-sm btn-sm-edit">编辑</Link>
                <button
                  className="btn-sm btn-sm-del"
                  onClick={() => handleDelete(article.id, article.title)}
                >
                  删除
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="admin-header admin-drafts-header">
        <h1>草稿箱</h1>
        <button className="btn btn-edit" onClick={() => setShowDrafts(!showDrafts)}>
          {showDrafts ? '收起' : '展开'}
        </button>
      </div>

      {showDrafts && (
        draftsLoading ? (
          <div className="page-status">加载中...</div>
        ) : drafts.length === 0 ? (
          <p className="empty">暂无草稿</p>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {drafts.map(draft => (
                <tr key={draft.id}>
                  <td>{draft.title || '(无标题)'}</td>
                  <td className="td-date">
                    {new Date(draft.updatedAt).toLocaleDateString('zh-CN')}
                  </td>
                  <td className="td-actions">
                    <Link to={`/editor?draftId=${draft.id}`} className="btn-sm btn-sm-edit">编辑</Link>
                    <button
                      className="btn-sm btn-sm-del"
                      onClick={() => handleDeleteDraft(draft.id, draft.title)}
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      )}
    </div>
  );
}
