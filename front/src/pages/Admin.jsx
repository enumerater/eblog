import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api';
import './Admin.css';

export default function Admin() {
  const [activeTab, setActiveTab] = useState('articles');
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [drafts, setDrafts] = useState([]);
  const [draftsLoading, setDraftsLoading] = useState(true);
  const [showDrafts, setShowDrafts] = useState(false);

  const [files, setFiles] = useState([]);
  const [filesLoading, setFilesLoading] = useState(false);

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

  const fetchFiles = () => {
    setFilesLoading(true);
    api.getFileList(null, 1, 20)
      .then(data => setFiles(data))
      .catch(() => {})
      .finally(() => setFilesLoading(false));
  };

  useEffect(() => { fetchArticles(); fetchDrafts(); }, []);

  useEffect(() => {
    if (activeTab === 'files') fetchFiles();
  }, [activeTab]);

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

  const handleDeleteFile = async (id, name) => {
    if (!window.confirm(`确定要删除文件「${name}」吗？`)) return;
    try {
      await api.deleteFile(id);
      setFiles(files.filter(f => f.id !== id));
    } catch (err) {
      alert('删除失败: ' + err.message);
    }
  };

  if (loading && activeTab === 'articles') return <div className="page-status">加载中...</div>;
  if (error) return <div className="page-status error">{error}</div>;

  return (
    <div className="admin-page">
      <div className="admin-header">
        <h1>管理后台</h1>
        <Link to="/editor" className="btn btn-edit">写新文章</Link>
      </div>

      {/* Stats Cards */}
      <div className="admin-stats">
        <div className="stat-card">
          <span className="stat-number">{articles.length}</span>
          <span className="stat-label">文章</span>
        </div>
        <div className="stat-card">
          <span className="stat-number">{drafts.length}</span>
          <span className="stat-label">草稿</span>
        </div>
        <div className="stat-card">
          <span className="stat-number">{files.length}</span>
          <span className="stat-label">文件</span>
        </div>
      </div>

      {/* Tabs */}
      <div className="admin-tabs">
        <button className={`admin-tab ${activeTab === 'articles' ? 'active' : ''}`}
                onClick={() => setActiveTab('articles')}>文章管理</button>
        <button className={`admin-tab ${activeTab === 'files' ? 'active' : ''}`}
                onClick={() => setActiveTab('files')}>文件管理</button>
      </div>

      {/* ── Articles Tab ── */}
      {activeTab === 'articles' && (
        <>
          {articles.length === 0 && <p className="empty">还没有文章</p>}

          <div className="admin-table-wrap">
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
                    <td data-label="标题">
                      <Link to={`/article/${article.id}`}>{article.title}</Link>
                    </td>
                    <td data-label="标签">
                      {article.tags?.length > 0
                        ? article.tags.map(t => <span key={t} className="tag">{t}</span>)
                        : <span className="td-empty">-</span>}
                    </td>
                    <td className="td-date" data-label="创建时间">
                      {new Date(article.createdAt).toLocaleDateString('zh-CN')}
                    </td>
                    <td className="td-actions" data-label="操作">
                      <Link to={`/editor?id=${article.id}`} className="btn-sm btn-sm-edit">编辑</Link>
                      <button className="btn-sm btn-sm-del"
                              onClick={() => handleDelete(article.id, article.title)}>删除</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

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
              <div className="admin-table-wrap">
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
                        <td data-label="标题">{draft.title || '(无标题)'}</td>
                        <td className="td-date" data-label="更新时间">
                          {new Date(draft.updatedAt).toLocaleDateString('zh-CN')}
                        </td>
                        <td className="td-actions" data-label="操作">
                          <Link to={`/editor?draftId=${draft.id}`} className="btn-sm btn-sm-edit">编辑</Link>
                          <button className="btn-sm btn-sm-del"
                                  onClick={() => handleDeleteDraft(draft.id, draft.title)}>删除</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}
        </>
      )}

      {/* ── Files Tab ── */}
      {activeTab === 'files' && (
        <>
          {filesLoading ? (
            <div className="page-status">加载中...</div>
          ) : files.length === 0 ? (
            <p className="empty">暂无文件</p>
          ) : (
            <div className="admin-table-wrap">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>文件名</th>
                    <th>类型</th>
                    <th>大小</th>
                    <th>上传时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {files.map(file => (
                    <tr key={file.id}>
                      <td data-label="文件名">{file.originalName}</td>
                      <td data-label="类型">{file.mimeType || '-'}</td>
                      <td data-label="大小">
                        {file.fileSize ? (file.fileSize / 1024).toFixed(1) + ' KB' : '-'}
                      </td>
                      <td className="td-date" data-label="上传时间">
                        {file.createdAt ? new Date(file.createdAt).toLocaleDateString('zh-CN') : '-'}
                      </td>
                      <td className="td-actions" data-label="操作">
                        <button className="btn-sm btn-sm-del"
                                onClick={() => handleDeleteFile(file.id, file.originalName)}>删除</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
