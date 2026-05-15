import { useState, useEffect, useRef, useMemo } from 'react';
import ArticleCard from '../components/ArticleCard';
import Footer from '../components/Footer';
import api from '../api';
import './Home.css';

export default function Home() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [activeTag, setActiveTag] = useState('');
  const searchTimerRef = useRef(null);

  // Fetch articles (with optional search params)
  const fetchArticles = (params = {}) => {
    setLoading(true);
    const hasParams = params.keyword || params.tag;
    const req = hasParams ? api.searchArticles(params) : api.getArticles();
    req
      .then(data => setArticles(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  };

  // Initial load
  useEffect(() => { fetchArticles(); }, []);

  // Debounced keyword search
  useEffect(() => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(() => {
      fetchArticles({ keyword, tag: activeTag });
    }, 300);
    return () => { if (searchTimerRef.current) clearTimeout(searchTimerRef.current); };
  }, [keyword, activeTag]); // eslint-disable-line react-hooks/exhaustive-deps

  // Collect all unique tags with counts from the full article set
  const allTags = useMemo(() => {
    const map = {};
    articles.forEach(a => {
      (a.tags || []).forEach(t => {
        map[t] = (map[t] || 0) + 1;
      });
    });
    return Object.entries(map).sort((a, b) => b[1] - a[1]);
  }, [articles]);

  const handleTagClick = (tag) => {
    setActiveTag(prev => prev === tag ? '' : tag);
  };

  return (
    <>
      <div className="home">
        <header className="home-header">
          <h1>随便写写</h1>
          <p className="home-subtitle">技术笔记 / 生活记录</p>
        </header>

        <div className="home-search-bar">
          <svg className="home-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="home-search-input"
            placeholder="搜索文章..."
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
          />
          {keyword && (
            <button className="home-search-clear" onClick={() => setKeyword('')}>&times;</button>
          )}
        </div>

        {allTags.length > 0 && (
          <div className="home-tags">
            {allTags.map(([tag, count]) => (
              <button
                key={tag}
                className={`home-tag-chip ${activeTag === tag ? 'active' : ''}`}
                onClick={() => handleTagClick(tag)}
              >
                {tag}
                <span className="home-tag-count">{count}</span>
              </button>
            ))}
          </div>
        )}

        <div className="home-layout">
          <main className="home-main">
            {loading ? (
              <div className="page-status">加载中...</div>
            ) : error ? (
              <div className="page-status error">加载失败: {error}</div>
            ) : articles.length === 0 ? (
              <p className="empty">
                {keyword || activeTag ? '没有找到匹配的文章' : '还没有文章，去<a href="/editor">写一篇</a>吧'}
              </p>
            ) : (
              <div className="article-list">
                {articles.map(article => (
                  <ArticleCard key={article.id} article={article} />
                ))}
              </div>
            )}
          </main>

          {allTags.length > 0 && (
            <aside className="home-sidebar">
              <div className="sidebar-section">
                <h3 className="sidebar-title">标签</h3>
                <div className="tag-cloud">
                  {allTags.map(([tag, count]) => (
                    <button
                      key={tag}
                      className={`tag-cloud-item ${activeTag === tag ? 'active' : ''}`}
                      onClick={() => handleTagClick(tag)}
                    >
                      {tag}
                      <span className="tag-cloud-count">{count}</span>
                    </button>
                  ))}
                </div>
              </div>
              <div className="sidebar-section">
                <h3 className="sidebar-title">统计</h3>
                <p className="sidebar-stat">共 {articles.length} 篇文章</p>
              </div>
            </aside>
          )}
        </div>
      </div>
      <Footer />
    </>
  );
}
