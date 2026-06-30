import { useState, useEffect, useRef, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import hljs from 'highlight.js/lib/core';
import javascript from 'highlight.js/lib/languages/javascript';
import typescript from 'highlight.js/lib/languages/typescript';
import python from 'highlight.js/lib/languages/python';
import java from 'highlight.js/lib/languages/java';
import go from 'highlight.js/lib/languages/go';
import rust from 'highlight.js/lib/languages/rust';
import c from 'highlight.js/lib/languages/c';
import cpp from 'highlight.js/lib/languages/cpp';
import csharp from 'highlight.js/lib/languages/csharp';
import php from 'highlight.js/lib/languages/php';
import ruby from 'highlight.js/lib/languages/ruby';
import swift from 'highlight.js/lib/languages/swift';
import kotlin from 'highlight.js/lib/languages/kotlin';
import sql from 'highlight.js/lib/languages/sql';
import bash from 'highlight.js/lib/languages/bash';
import json from 'highlight.js/lib/languages/json';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';
import css from 'highlight.js/lib/languages/css';
import scss from 'highlight.js/lib/languages/scss';
import markdown from 'highlight.js/lib/languages/markdown';
import nginx from 'highlight.js/lib/languages/nginx';
import lua from 'highlight.js/lib/languages/lua';
import perl from 'highlight.js/lib/languages/perl';
import Footer from '../components/Footer';
import CommentSection from '../components/CommentSection';
import ReadingProgress from '../components/effects/ReadingProgress';
import api, { auth } from '../api';
import './ArticleDetail.css';

hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('python', python);
hljs.registerLanguage('java', java);
hljs.registerLanguage('go', go);
hljs.registerLanguage('rust', rust);
hljs.registerLanguage('c', c);
hljs.registerLanguage('cpp', cpp);
hljs.registerLanguage('csharp', csharp);
hljs.registerLanguage('php', php);
hljs.registerLanguage('ruby', ruby);
hljs.registerLanguage('swift', swift);
hljs.registerLanguage('kotlin', kotlin);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('shell', bash);
hljs.registerLanguage('json', json);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('yaml', yaml);
hljs.registerLanguage('css', css);
hljs.registerLanguage('scss', scss);
hljs.registerLanguage('html', xml);
hljs.registerLanguage('markdown', markdown);
hljs.registerLanguage('nginx', nginx);
hljs.registerLanguage('lua', lua);
hljs.registerLanguage('perl', perl);

function slugify(text) {
  return text.trim().toLowerCase().replace(/\s+/g, '-').replace(/[^\w一-鿿-]/g, '').slice(0, 60) || 'heading';
}

function parseToc(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const headings = doc.querySelectorAll('h1, h2, h3');
  const usedIds = new Set();
  return Array.from(headings).map(h => {
    const text = h.textContent.trim();
    let baseId = slugify(text);
    let id = baseId;
    let counter = 1;
    while (usedIds.has(id)) {
      id = `${baseId}-${counter++}`;
    }
    usedIds.add(id);
    return { id, text, level: h.tagName === 'H1' ? 1 : h.tagName === 'H2' ? 2 : 3 };
  });
}

export default function ArticleDetail() {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const contentRef = useRef(null);

  useEffect(() => {
    api.getArticleDetail(id)
      .then(data => setArticle(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));

    // 并行获取智能数据
    api.getArticleStats(id).then(d => setStats(d)).catch(() => {});
    api.getRecommendations(id, 5).then(d => setRecommendations(d || [])).catch(() => {});
  }, [id]);

  // Parse TOC from HTML string — no state, no re-render
  const toc = useMemo(() => {
    if (!article?.content) return [];
    return parseToc(article.content);
  }, [article?.content]);

  const activeRef = useRef('');
  const tocRef = useRef(null);

  // Assign heading IDs, highlight code, set up scroll tracking
  useEffect(() => {
    if (!article || !contentRef.current || toc.length === 0) return;

    const headings = contentRef.current.querySelectorAll('h2, h3');
    headings.forEach((h, i) => {
      if (toc[i]) h.id = toc[i].id;
    });

    // Highlight code blocks
    contentRef.current.querySelectorAll('pre code').forEach(block => {
      const langClass = [...block.classList].find(c => c.startsWith('language-'));
      if (langClass) {
        block.setAttribute('data-language', langClass.replace('language-', ''));
      }
      hljs.highlightElement(block);
    });

    // Scroll-based active heading — update TOC via DOM, no re-render
    const tocLinks = tocRef.current?.querySelectorAll('.toc-link');
    const setActive = (id) => {
      if (id === activeRef.current) return;
      activeRef.current = id;
      tocLinks?.forEach(link => {
        link.classList.toggle('active', link.getAttribute('data-id') === id);
      });
    };

    const onScroll = () => {
      // Query fresh each time (React may replace DOM nodes on re-render)
      const h2h3 = contentRef.current?.querySelectorAll('h2, h3');
      if (!h2h3) return;
      let current = '';
      for (const h of h2h3) {
        if (h.getBoundingClientRect().top <= 88) {
          current = h.id;
        }
      }
      setActive(current);
    };

    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, [article, toc]);

  const handleTocClick = (e, headingId) => {
    e.preventDefault();
    // Query fresh — DOM may have been replaced by React
    const el = contentRef.current?.querySelector(`#${CSS.escape(headingId)}`);
    if (el) {
      const y = el.getBoundingClientRect().top + window.scrollY - 80;
      window.scrollTo({ top: y, behavior: 'smooth' });
    }
  };

  if (loading) return <div className="page-status">加载中...</div>;
  if (error) return <div className="page-status error">{error}</div>;
  if (!article) return <div className="page-status">文章不存在</div>;

  const date = new Date(article.createdAt).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric',
  });

  return (
    <>
      <ReadingProgress />
      <div className="article-detail">
        <Link to="/" className="back-link">返回首页</Link>
        <div className="article-layout">
          <article>
            <h1 className="detail-title">{article.title}</h1>
            <div className="detail-meta">
              <span className="detail-date">{date}</span>
              {article.tags?.length > 0 && (
                <span className="detail-tags">
                  {article.tags.map(t => <span key={t} className="tag">{t}</span>)}
                </span>
              )}
            </div>
            <div
              ref={contentRef}
              className="detail-content"
              dangerouslySetInnerHTML={{ __html: article.content }}
            />

            {/* ── 文章统计 (intelligence-service) ── */}
            {stats && (
              <div className="detail-stats">
                <span title="总字数">📝 {stats.wordCount?.toLocaleString()} 字</span>
                <span title="阅读时长">⏱ 约 {stats.readingTimeMinutes} 分钟</span>
                {stats.paragraphCount > 0 && <span title="段落数">¶ {stats.paragraphCount} 段</span>}
                {stats.imageCount > 0 && <span title="图片数">🖼 {stats.imageCount} 张图</span>}
                {stats.keywords?.length > 0 && (
                  <span title="关键词">
                    🏷 {stats.keywords.slice(0, 5).join(' · ')}
                  </span>
                )}
              </div>
            )}

            {/* ── 相关推荐 (intelligence-service) ── */}
            {recommendations.length > 0 && (
              <div className="detail-recommendations">
                <h3>相关推荐</h3>
                <div className="recommendation-list">
                  {recommendations.map(r => (
                    <Link key={r.id} to={`/article/${r.id}`} className="recommendation-item">
                      <span className="rec-title">{r.title}</span>
                      <span className="rec-score">{(r.score * 100).toFixed(0)}%</span>
                    </Link>
                  ))}
                </div>
              </div>
            )}
          </article>
          {toc.length > 0 && (
            <aside className="toc-sidebar">
              <nav className="toc-nav" ref={tocRef}>
                {toc.map(item => (
                  <a
                    key={item.id}
                    href={`#${item.id}`}
                    data-id={item.id}
                    className={`toc-link toc-level-${item.level}`}
                    onClick={e => handleTocClick(e, item.id)}
                  >
                    {item.text}
                  </a>
                ))}
              </nav>
            </aside>
          )}
        </div>
        {auth.isAuthed() && (
          <div className="detail-actions">
            <Link to={`/editor?id=${article.id}`} className="btn btn-edit">编辑文章</Link>
          </div>
        )}
        <CommentSection articleId={article.id} />
      </div>
      <Footer />
    </>
  );
}
