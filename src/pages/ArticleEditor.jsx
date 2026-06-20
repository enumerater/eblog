import { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import api from '../api';
import RichEditor from '../components/editor/RichEditor';
import './ArticleEditor.css';

function parseToc(html) {
  if (!html) return [];
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const headings = doc.querySelectorAll('h2, h3');
  const usedIds = new Set();
  return Array.from(headings).map(h => {
    const text = h.textContent.trim();
    const base = text.toLowerCase().replace(/\s+/g, '-').replace(/[^\w一-鿿-]/g, '').slice(0, 60) || 'heading';
    let id = base, c = 1;
    while (usedIds.has(id)) id = `${base}-${c++}`;
    usedIds.add(id);
    return { id, text, level: h.tagName === 'H2' ? 2 : 3 };
  });
}

export default function ArticleEditor() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const editId = searchParams.get('id');
  const draftIdParam = searchParams.get('draftId');
  const isEdit = !!editId;

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [tags, setTags] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEdit || !!draftIdParam);
  const [error, setError] = useState(null);
  const [toc, setToc] = useState([]);
  const [draftId, setDraftId] = useState(draftIdParam ? Number(draftIdParam) : null);
  const [saveStatus, setSaveStatus] = useState(null); // null | 'saving' | 'saved' | 'error'
  const activeRef = useRef('');
  const tocRef = useRef(null);
  const scrollRef = useRef(null);
  const saveTimerRef = useRef(null);
  const lastSavedRef = useRef('');
  const isDirtyRef = useRef(false);
  const saveStatusTimerRef = useRef(null);

  const uploadImage = useCallback(async (file) => {
    const result = await api.uploadImage(file);
    return result.url;
  }, []);

  // Load draft or article on mount
  useEffect(() => {
    if (draftIdParam) {
      api.getDraft(draftIdParam)
        .then(draft => {
          setTitle(draft.title || '');
          setContent(draft.content || '');
          setTags((draft.tags || []).join(', '));
          setDraftId(draft.id);
          // Snapshot the loaded state so auto-save doesn't fire immediately
          const snap = JSON.stringify({ t: draft.title || '', c: draft.content || '', g: (draft.tags || []).join(', ') });
          lastSavedRef.current = snap;
        })
        .catch(err => setError(err.message))
        .finally(() => setFetching(false));
    } else if (editId) {
      api.getArticle(editId)
        .then(article => {
          setTitle(article.title);
          setContent(article.content || '');
          setTags((article.tags || []).join(', '));
        })
        .catch(err => setError(err.message))
        .finally(() => setFetching(false));
    }
  }, [draftIdParam, editId]);

  // Debounce TOC parsing
  useEffect(() => {
    const timer = setTimeout(() => {
      setToc(parseToc(content));
    }, 400);
    return () => clearTimeout(timer);
  }, [content]);

  // Auto-save logic
  const performSave = useCallback(async () => {
    const currentTitle = title;
    const currentContent = content;
    const currentTags = tags;

    // Skip if all empty
    if (!currentTitle.trim() && !currentContent.trim() && !currentTags.trim()) return;

    // Skip if unchanged
    const snap = JSON.stringify({ t: currentTitle, c: currentContent, g: currentTags });
    if (snap === lastSavedRef.current) return;

    // Skip if already saving
    if (saveStatus === 'saving') return;

    setSaveStatus('saving');
    try {
      const data = {
        title: currentTitle.trim(),
        content: currentContent.trim(),
        tags: currentTags.split(',').map(t => t.trim()).filter(Boolean),
      };

      if (draftId) {
        await api.updateDraft(draftId, data);
      } else {
        const created = await api.createDraft(data);
        setDraftId(created.id);
      }

      lastSavedRef.current = snap;
      isDirtyRef.current = false;
      setSaveStatus('saved');

      // Clear "saved" status after 3 seconds
      if (saveStatusTimerRef.current) clearTimeout(saveStatusTimerRef.current);
      saveStatusTimerRef.current = setTimeout(() => setSaveStatus(null), 3000);
    } catch (err) {
      setSaveStatus('error');
      if (saveStatusTimerRef.current) clearTimeout(saveStatusTimerRef.current);
      saveStatusTimerRef.current = setTimeout(() => setSaveStatus(null), 5000);
    }
  }, [title, content, tags, draftId, saveStatus]);

  // Auto-save on content change (2s debounce)
  useEffect(() => {
    if (fetching) return; // Don't auto-save while loading initial data

    isDirtyRef.current = true;
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    saveTimerRef.current = setTimeout(() => {
      performSave();
    }, 2000);

    return () => {
      if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    };
  }, [title, content, tags, fetching]); // eslint-disable-line react-hooks/exhaustive-deps

  // Ctrl+S handler
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
        performSave();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [performSave]);

  // Warn before leaving with unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (isDirtyRef.current) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, []);

  // Cleanup status timer on unmount
  useEffect(() => {
    return () => {
      if (saveStatusTimerRef.current) clearTimeout(saveStatusTimerRef.current);
    };
  }, []);

  // Scroll-based active heading tracking for editor
  useEffect(() => {
    if (toc.length === 0) {
      tocRef.current?.querySelectorAll('.toc-link').forEach(l => l.classList.remove('active'));
      if (scrollRef.current) {
        const { el, fn } = scrollRef.current;
        el.removeEventListener('scroll', fn);
        scrollRef.current = null;
      }
      return;
    }

    const setActive = (id) => {
      if (id === activeRef.current) return;
      activeRef.current = id;
      tocRef.current?.querySelectorAll('.toc-link').forEach(link => {
        link.classList.toggle('active', link.getAttribute('data-id') === id);
      });
    };

    const onScroll = () => {
      const scrollEl = document.querySelector('.editor-content');
      const editorEl = scrollEl?.querySelector('.ProseMirror');
      if (!editorEl) return;

      const containerRect = scrollEl.getBoundingClientRect();
      let current = '';

      editorEl.querySelectorAll('h2, h3').forEach(h => {
        const rect = h.getBoundingClientRect();
        if (rect.top <= containerRect.top + 60) {
          const text = h.textContent.trim();
          const match = toc.find(t => t.text === text);
          if (match) current = match.id;
        }
      });
      setActive(current);
    };

    if (scrollRef.current) {
      const { el, fn } = scrollRef.current;
      el.removeEventListener('scroll', fn);
    }

    const scrollEl = document.querySelector('.editor-content');
    if (scrollEl) {
      onScroll();
      scrollEl.addEventListener('scroll', onScroll, { passive: true });
      scrollRef.current = { el: scrollEl, fn: onScroll };
    }

    return () => {
      if (scrollRef.current) {
        const { el, fn } = scrollRef.current;
        el.removeEventListener('scroll', fn);
        scrollRef.current = null;
      }
    };
  }, [toc]);

  const handleTocClick = (e, headingId) => {
    e.preventDefault();
    const tocItem = toc.find(t => t.id === headingId);
    if (!tocItem) return;

    const scrollEl = document.querySelector('.editor-content');
    const editorEl = scrollEl?.querySelector('.ProseMirror');
    if (!scrollEl || !editorEl) return;

    const headings = editorEl.querySelectorAll('h2, h3');
    for (const h of headings) {
      if (h.textContent.trim() === tocItem.text) {
        h.scrollIntoView({ behavior: 'smooth', block: 'start' });
        break;
      }
    }
  };

  const handleSaveDraft = () => {
    if (saveTimerRef.current) clearTimeout(saveTimerRef.current);
    performSave();
  };

  const handlePublish = async (e) => {
    e.preventDefault();
    if (!title.trim()) { setError('请输入文章标题'); return; }
    if (!content.trim()) { setError('请输入文章内容'); return; }

    setLoading(true);
    setError(null);

    const data = {
      title: title.trim(),
      content: content.trim(),
      tags: tags.split(',').map(t => t.trim()).filter(Boolean),
    };

    try {
      if (draftId) {
        await api.publishDraft(draftId, data);
      } else if (isEdit) {
        await api.updateArticle(editId, data);
      } else {
        await api.createArticle(data);
      }
      isDirtyRef.current = false;
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) { setError('请输入文章标题'); return; }
    if (!content.trim()) { setError('请输入文章内容'); return; }

    setLoading(true);
    setError(null);

    const data = {
      title: title.trim(),
      content: content.trim(),
      tags: tags.split(',').map(t => t.trim()).filter(Boolean),
    };

    try {
      if (isEdit) {
        await api.updateArticle(editId, data);
      } else {
        await api.createArticle(data);
      }
      isDirtyRef.current = false;
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (fetching) return <div className="page-status">加载中...</div>;

  const pageTitle = draftId ? '编辑草稿' : isEdit ? '编辑文章' : '写文章';
  const isDraftMode = !!draftIdParam || (draftId && !isEdit);

  return (
    <div className="editor-page">
      <h1>{pageTitle}</h1>
      {error && <div className="editor-error">{error}</div>}
      <div className="editor-layout">
        <form onSubmit={isDraftMode ? handlePublish : handleSubmit} className="editor-form">
          <div className="form-group">
            <label>标题</label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="文章标题"
            />
          </div>
          <div className="form-group">
            <label>标签</label>
            <input
              type="text"
              value={tags}
              onChange={e => setTags(e.target.value)}
              placeholder="用逗号分隔，如：React, JavaScript, 前端"
            />
          </div>
          <div className="form-group">
            <label>内容</label>
            <RichEditor content={content} onChange={setContent} onUploadImage={uploadImage} />
          </div>
          <div className="form-actions">
            <button type="button" className="btn btn-draft" onClick={handleSaveDraft}>
              保存草稿
            </button>
            {saveStatus && (
              <span className={`draft-save-status draft-save-${saveStatus}`}>
                {saveStatus === 'saving' && '保存中...'}
                {saveStatus === 'saved' && '草稿已保存'}
                {saveStatus === 'error' && '保存失败'}
              </span>
            )}
            <button type="submit" className="btn btn-submit" disabled={loading}>
              {loading ? '提交中...' : isDraftMode ? '发布文章' : isEdit ? '更新文章' : '发布文章'}
            </button>
            <button type="button" className="btn btn-cancel" onClick={() => navigate('/')}>
              取消
            </button>
          </div>
        </form>
        {toc.length > 0 && (
          <aside className="editor-toc-sidebar">
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
    </div>
  );
}
