import { useState, useEffect, useRef, useMemo } from 'react';
import ArticleCard from '../components/ArticleCard';
import Footer from '../components/Footer';
import ParticleBg from '../components/effects/ParticleBg';
import Typewriter from '../components/effects/Typewriter';
import MouseGlow from '../components/effects/MouseGlow';
import api from '../api';
import './Home.css';

const PAGE_SIZE = 10;

export default function Home() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [activeTag, setActiveTag] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const searchTimerRef = useRef(null);

  // Fetch articles with pagination
  const fetchArticles = (params = {}) => {
    setLoading(true);
    const p = params.page ?? page;
    const hasParams = params.keyword || params.tag;

    const req = hasParams
      ? api.searchArticles({ ...params, page: p, size: PAGE_SIZE })
      : api.getArticles(p, PAGE_SIZE);

    req
      .then(data => {
        // data 可能是分页对象 { content, totalPages, totalElements, page }
        // 也可能是数组（兼容旧返回）
        if (Array.isArray(data)) {
          setArticles(data);
          setTotalPages(1);
          setTotalElements(data.length);
        } else {
          setArticles(data.content || []);
          setTotalPages(data.totalPages || 0);
          setTotalElements(data.totalElements || 0);
        }
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  };

  // Initial load
  useEffect(() => { fetchArticles({ page: 0 }); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Debounced keyword/tag search (reset to page 0)
  useEffect(() => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(() => {
      setPage(0);
      fetchArticles({ keyword, tag: activeTag, page: 0 });
    }, 300);
    return () => { if (searchTimerRef.current) clearTimeout(searchTimerRef.current); };
  }, [keyword, activeTag]); // eslint-disable-line react-hooks/exhaustive-deps

  // Collect all unique tags with counts from the current page
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

  // Pagination handlers
  const goToPage = (p) => {
    if (p < 0 || p >= totalPages) return;
    setPage(p);
    fetchArticles({ keyword, tag: activeTag, page: p });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Generate page numbers to display
  const pageNumbers = useMemo(() => {
    const pages = [];
    const maxVisible = 5;
    let start = Math.max(0, page - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible);
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }, [page, totalPages]);

  return (
    <>
      <div className="home">
        <header className="home-header">
          <div className="home-header-bg">
            <ParticleBg />
          </div>
          <div className="home-header-content">
            <h1>随便写写</h1>
            <p className="home-subtitle">
              <Typewriter
                texts={['技术笔记 / 生活记录', '保持思考，持续输出', '记录每一个灵感']}
                speed={80}
                pause={2500}
              />
            </p>
          </div>
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
              <>
                <div className="article-list">
                  <MouseGlow color="rgba(192,57,43,0.06)" size="300">
                    {articles.map(article => (
                      <ArticleCard key={article.id} article={article} />
                    ))}
                  </MouseGlow>
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="pagination">
                    <button
                      className="pagination-btn"
                      disabled={page === 0}
                      onClick={() => goToPage(0)}
                      title="第一页"
                    >
                      &laquo;
                    </button>
                    <button
                      className="pagination-btn"
                      disabled={page === 0}
                      onClick={() => goToPage(page - 1)}
                      title="上一页"
                    >
                      &lsaquo;
                    </button>
                    {pageNumbers.map(p => (
                      <button
                        key={p}
                        className={`pagination-btn ${p === page ? 'active' : ''}`}
                        onClick={() => goToPage(p)}
                      >
                        {p + 1}
                      </button>
                    ))}
                    <button
                      className="pagination-btn"
                      disabled={page >= totalPages - 1}
                      onClick={() => goToPage(page + 1)}
                      title="下一页"
                    >
                      &rsaquo;
                    </button>
                    <button
                      className="pagination-btn"
                      disabled={page >= totalPages - 1}
                      onClick={() => goToPage(totalPages - 1)}
                      title="最后一页"
                    >
                      &raquo;
                    </button>
                  </div>
                )}
              </>
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
                <p className="sidebar-stat">共 {totalElements} 篇文章</p>
              </div>
            </aside>
          )}
        </div>
      </div>
      <Footer />
    </>
  );
}