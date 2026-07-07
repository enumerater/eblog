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
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [activeTag, setActiveTag] = useState('');
  const [hotSearches, setHotSearches] = useState([]);
  const [hotArticles, setHotArticles] = useState([]);
  const [searchSuggestions, setSearchSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const searchTimerRef = useRef(null);
  const suggestTimerRef = useRef(null);

  // Fetch articles — query-service 分页
  const fetchArticles = (params = {}) => {
    setLoading(true);
    const p = params.page || 1;
    const kw = params.keyword || '';
    const tag = params.tag || '';

    if (kw) {
      // 有搜索词用 search-service
      api.searchFullText({ keyword: kw, tag, page: p, size: PAGE_SIZE })
        .then(data => {
          // search-service 返回的是数组
          setArticles(Array.isArray(data) ? data : data?.items || data || []);
          setTotal(data?.total ?? articles.length);
          setTotalPages(data?.totalPages ?? 1);
        })
        .catch(() => {
          // fallback 到 query-service
          api.getArticlesPage(p, PAGE_SIZE, tag)
            .then(res => {
              setArticles(res?.items || []);
              setTotal(res?.total || 0);
              setTotalPages(res?.totalPages || 1);
            })
            .catch(() => {});
        })
        .finally(() => setLoading(false));
    } else {
      // 无搜索词用 query-service 分页
      api.getArticlesPage(p, PAGE_SIZE, tag)
        .then(res => {
          setArticles(res?.items || []);
          setTotal(res?.total || 0);
          setTotalPages(res?.totalPages || 1);
        })
        .catch(err => setError(err.message))
        .finally(() => setLoading(false));
    }
  };

  // Initial load
  useEffect(() => {
    fetchArticles();
    api.getHotSearches(5).then(d => setHotSearches(d)).catch(() => {});
    api.getHotArticles(5).then(d => setHotArticles(d)).catch(() => {});
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // 搜索/标签变更 → 重置到第1页
  useEffect(() => {
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(() => {
      setPage(1);
      fetchArticles({ keyword, tag: activeTag, page: 1 });
    }, 300);
    return () => { if (searchTimerRef.current) clearTimeout(searchTimerRef.current); };
  }, [keyword, activeTag]); // eslint-disable-line react-hooks/exhaustive-deps

  // 页码切换
  const goToPage = (p) => {
    if (p < 1 || p > totalPages) return;
    setPage(p);
    fetchArticles({ keyword, tag: activeTag, page: p });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Fetch suggestions
  useEffect(() => {
    if (suggestTimerRef.current) clearTimeout(suggestTimerRef.current);
    if (keyword && keyword.length >= 1) {
      suggestTimerRef.current = setTimeout(async () => {
        try {
          const data = await api.getSearchSuggestions(keyword);
          setSearchSuggestions(data || []);
          setShowSuggestions(data && data.length > 0);
        } catch { setShowSuggestions(false); }
      }, 200);
    } else {
      setShowSuggestions(false);
    }
    return () => { if (suggestTimerRef.current) clearTimeout(suggestTimerRef.current); };
  }, [keyword]);

  // 标签云从 query-service 获取
  const [tagCloud, setTagCloud] = useState([]);
  useEffect(() => {
    api.getTagCloud().then(data => setTagCloud(data || [])).catch(() => {});
  }, []);

  const handleTagClick = (tag) => {
    setActiveTag(prev => prev === tag ? '' : tag);
  };

  const selectSuggestion = (suggestion) => {
    setKeyword(suggestion);
    setShowSuggestions(false);
  };

  // 分页按钮
  const renderPagination = () => {
    if (totalPages <= 1) return null;
    const buttons = [];
    const range = 2;
    const start = Math.max(1, page - range);
    const end = Math.min(totalPages, page + range);

    if (start > 1) {
      buttons.push(<button key={1} className="page-btn" onClick={() => goToPage(1)}>1</button>);
      if (start > 2) buttons.push(<span key="dots1" className="page-dots">…</span>);
    }
    for (let i = start; i <= end; i++) {
      buttons.push(
        <button key={i} className={`page-btn ${i === page ? 'active' : ''}`} onClick={() => goToPage(i)}>
          {i}
        </button>
      );
    }
    if (end < totalPages) {
      if (end < totalPages - 1) buttons.push(<span key="dots2" className="page-dots">…</span>);
      buttons.push(
        <button key={totalPages} className="page-btn" onClick={() => goToPage(totalPages)}>
          {totalPages}
        </button>
      );
    }
    return <div className="pagination">{buttons}</div>;
  };

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

        <div className="home-search-bar" style={{ position: 'relative' }}>
          <svg className="home-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="home-search-input"
            placeholder="搜索文章..."
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onFocus={() => keyword && searchSuggestions.length > 0 && setShowSuggestions(true)}
            onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
          />
          {keyword && (
            <button className="home-search-clear" onClick={() => setKeyword('')}>&times;</button>
          )}
          {showSuggestions && (
            <div className="search-suggestions-dropdown">
              {searchSuggestions.map((s, i) => (
                <div key={i} className="search-suggestion-item" onMouseDown={() => selectSuggestion(s.keyword || s)}>
                  {s.keyword || s}
                </div>
              ))}
            </div>
          )}
        </div>

        {tagCloud.length > 0 && (
          <div className="home-tags">
            {tagCloud.map(({ tag, count }) => (
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
                {renderPagination()}
              </>
            )}
          </main>

          <aside className="home-sidebar">
            {tagCloud.length > 0 && (
              <div className="sidebar-section">
                <h3 className="sidebar-title">标签</h3>
                <div className="tag-cloud">
                  {tagCloud.map(({ tag, count }) => (
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
            )}

            {hotSearches.length > 0 && (
              <div className="sidebar-section">
                <h3 className="sidebar-title">热搜</h3>
                <div className="hot-search-list">
                  {hotSearches.map((item, idx) => (
                    <button key={idx} className="hot-search-item"
                            onClick={() => setKeyword(item.keyword || item)}>
                      <span className="hot-search-rank">{idx + 1}</span>
                      <span className="hot-search-word">{item.keyword || item}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {hotArticles.length > 0 && (
              <div className="sidebar-section">
                <h3 className="sidebar-title">热门文章</h3>
                <div className="hot-article-list">
                  {hotArticles.map((article, idx) => (
                    <a key={article.id || idx} href={`/article/${article.id}`} className="hot-article-item">
                      <span className="hot-article-rank">{idx + 1}</span>
                      <span className="hot-article-title">{article.title}</span>
                    </a>
                  ))}
                </div>
              </div>
            )}

            <div className="sidebar-section">
              <h3 className="sidebar-title">统计</h3>
              <p className="sidebar-stat">共 {total} 篇文章</p>
            </div>
          </aside>
        </div>
      </div>
      <Footer />
    </>
  );
}