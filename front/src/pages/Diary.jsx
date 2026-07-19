import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { marked } from 'marked';
import api from '../api';
import './Diary.css';

// 心情选项
const MOODS = [
  { emoji: '😊', label: '开心' },
  { emoji: '😐', label: '一般' },
  { emoji: '😢', label: '难过' },
  { emoji: '😴', label: '疲惫' },
  { emoji: '🔥', label: '充满干劲' },
  { emoji: '🥰', label: '甜蜜' },
  { emoji: '🤔', label: '思考中' },
  { emoji: '😤', label: '烦躁' },
];

// 天气选项
const WEATHERS = [
  { icon: '☀️', label: '晴' },
  { icon: '⛅', label: '多云' },
  { icon: '🌧️', label: '雨' },
  { icon: '🌨️', label: '雪' },
  { icon: '🌫️', label: '雾' },
  { icon: '🌪️', label: '大风' },
  { icon: '🌈', label: '彩虹' },
];

const WEEKDAYS = ['一', '二', '三', '四', '五', '六', '日'];
const MONTHS = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];

function formatDate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function formatDisplayDate(dateStr) {
  const d = new Date(dateStr + 'T00:00:00');
  const week = WEEKDAYS[d.getDay() === 0 ? 6 : d.getDay() - 1];
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`;
}

export default function Diary() {
  const navigate = useNavigate();
  const today = new Date();
  const todayStr = formatDate(today);

  const [diaries, setDiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 日历状态
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());

  // 选中日期
  const [selectedDate, setSelectedDate] = useState(todayStr);
  const [currentDiary, setCurrentDiary] = useState(null);
  const [diaryLoading, setDiaryLoading] = useState(false);

  // 编辑状态
  const [isEditing, setIsEditing] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [editContent, setEditContent] = useState('');
  const [editMood, setEditMood] = useState('');
  const [editWeather, setEditWeather] = useState('');
  const [saving, setSaving] = useState(false);

  const textareaRef = useRef(null);

  // 加载所有日记日期
  const fetchDiaries = useCallback(() => {
    setLoading(true);
    setError(null);
    api.getDiaries()
      .then(data => setDiaries(data || []))
      .catch(err => setError(err.message || '加载失败'))
      .finally(() => setLoading(false));
  }, []);

  // 加载选中日期的日记
  const fetchDiaryByDate = useCallback((dateStr) => {
    setDiaryLoading(true);
    setCurrentDiary(null);
    api.getDiaryByDate(dateStr)
      .then(data => setCurrentDiary(data))
      .catch(() => setCurrentDiary(null))
      .finally(() => setDiaryLoading(false));
  }, []);

  useEffect(() => {
    fetchDiaries();
  }, [fetchDiaries]);

  useEffect(() => {
    if (!loading) {
      fetchDiaryByDate(selectedDate);
    }
  }, [selectedDate, loading, fetchDiaryByDate]);

  // 切换日期
  const handleSelectDate = (dateStr) => {
    if (dateStr === selectedDate) return;
    setSelectedDate(dateStr);
    setIsEditing(false);
    setShowPreview(false);
  };

  // 开始写日记
  const handleStartWrite = () => {
    setEditContent('');
    setEditMood('');
    setEditWeather('');
    setShowPreview(false);
    setIsEditing(true);
    setTimeout(() => textareaRef.current?.focus(), 100);
  };

  // 编辑已有日记
  const handleEdit = () => {
    if (!currentDiary) return;
    setEditContent(currentDiary.content);
    setEditMood(currentDiary.mood || '');
    setEditWeather(currentDiary.weather || '');
    setShowPreview(false);
    setIsEditing(true);
    setTimeout(() => textareaRef.current?.focus(), 100);
  };

  // 取消编辑
  const handleCancelEdit = () => {
    setIsEditing(false);
    setShowPreview(false);
  };

  // 保存日记
  const handleSave = async () => {
    if (!editContent.trim()) return;
    setSaving(true);
    try {
      const data = { date: selectedDate, content: editContent };
      if (editMood) data.mood = editMood;
      if (editWeather) data.weather = editWeather;

      if (currentDiary) {
        const updated = await api.updateDiary(currentDiary.id, data);
        setCurrentDiary(updated);
      } else {
        const created = await api.createDiary(data);
        setCurrentDiary(created);
        // 更新日历标记
        setDiaries(prev => [created, ...prev].sort((a, b) => b.date.localeCompare(a.date)));
      }
      setIsEditing(false);
      setShowPreview(false);
    } catch (err) {
      alert('保存失败: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  // 删除日记
  const handleDelete = async () => {
    if (!currentDiary) return;
    if (!window.confirm(`确定要删除 ${formatDisplayDate(selectedDate)} 的日记吗？此操作不可撤销。`)) return;
    try {
      await api.deleteDiary(currentDiary.id);
      setDiaries(prev => prev.filter(d => d.id !== currentDiary.id));
      setCurrentDiary(null);
      setIsEditing(false);
      setShowPreview(false);
    } catch (err) {
      alert('删除失败: ' + err.message);
    }
  };

  // 日历导航
  const prevMonth = () => {
    if (viewMonth === 0) {
      setViewYear(y => y - 1);
      setViewMonth(11);
    } else {
      setViewMonth(m => m - 1);
    }
  };

  const nextMonth = () => {
    if (viewMonth === 11) {
      setViewYear(y => y + 1);
      setViewMonth(0);
    } else {
      setViewMonth(m => m + 1);
    }
  };

  const goToday = () => {
    setViewYear(today.getFullYear());
    setViewMonth(today.getMonth());
    setSelectedDate(todayStr);
    setIsEditing(false);
    setShowPreview(false);
  };

  // 生成日历格子
  const hasDiaryOnDate = (dateStr) => diaries.some(d => d.date === dateStr);
  const isToday = (dateStr) => dateStr === todayStr;
  const isSelected = (dateStr) => dateStr === selectedDate;

  const buildCalendarDays = () => {
    const firstDay = new Date(viewYear, viewMonth, 1);
    const lastDay = new Date(viewYear, viewMonth + 1, 0);
    // 0=Sun, 1=Mon, ... 6=Sat → 我们想要周一为一周第一天
    let startDow = firstDay.getDay() - 1;
    if (startDow < 0) startDow = 6; // 周日是最后一天

    const days = [];
    // 填充日期
    for (let d = 1; d <= lastDay.getDate(); d++) {
      const date = new Date(viewYear, viewMonth, d);
      days.push(formatDate(date));
    }
    return { days, startDow };
  };

  const { days: calendarDays, startDow } = buildCalendarDays();

  // 渲染 Markdown
  const renderMarkdown = (content) => {
    if (!content) return '';
    try {
      return marked(content, { breaks: true });
    } catch {
      return content;
    }
  };

  if (loading) {
    return (
      <div className="diary-page">
        <div className="page-status">加载中...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="diary-page">
        <div className="page-status error">{error}</div>
      </div>
    );
  }

  const diaryDates = new Set(diaries.map(d => d.date));

  return (
    <div className="diary-page">
      <div className="diary-header">
        <h1 className="diary-title">📔 我的日记</h1>
        <div className="diary-header-actions">
          <button className="btn btn-today" onClick={goToday}>📅 今天</button>
          {!isEditing && (
            <button className="btn btn-write" onClick={handleStartWrite}>✍️ 写日记</button>
          )}
        </div>
      </div>

      <div className="diary-body">
        {/* 日历侧边栏 */}
        <aside className="diary-sidebar">
          <div className="diary-calendar">
            <div className="calendar-nav">
              <button className="calendar-nav-btn" onClick={prevMonth}>&lsaquo;</button>
              <span className="calendar-nav-title">{viewYear}年 {MONTHS[viewMonth]}</span>
              <button className="calendar-nav-btn" onClick={nextMonth}>&rsaquo;</button>
            </div>
            <div className="calendar-weekdays">
              {WEEKDAYS.map(w => <span key={w} className="calendar-weekday">{w}</span>)}
            </div>
            <div className="calendar-grid">
              {calendarDays.map((dateStr, i) => {
                const has = diaryDates.has(dateStr);
                const sel = isSelected(dateStr);
                const t = isToday(dateStr);
                const style = i === 0 && startDow > 0 ? { gridColumnStart: startDow + 1 } : {};
                return (
                  <button
                    key={dateStr}
                    className={`calendar-day ${sel ? 'selected' : ''} ${t ? 'today' : ''} ${has ? 'has-entry' : ''}`}
                    style={style}
                    onClick={() => handleSelectDate(dateStr)}
                  >
                    {new Date(dateStr + 'T00:00:00').getDate()}
                    {has && <span className="calendar-dot" />}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="diary-legend">
            <span className="legend-item"><span className="legend-dot filled" /> 有日记</span>
            <span className="legend-item"><span className="legend-dot today-dot" /> 今天</span>
          </div>
        </aside>

        {/* 主内容区 */}
        <main className="diary-content">
          <div className="diary-content-header">
            <span className="diary-date-label">{formatDisplayDate(selectedDate)}</span>
            {currentDiary && !isEditing && (
              <div className="diary-meta-tags">
                {currentDiary.mood && <span className="diary-tag">{currentDiary.mood}</span>}
                {currentDiary.weather && <span className="diary-tag">{currentDiary.weather}</span>}
              </div>
            )}
          </div>

          <div className="diary-content-body">
            {diaryLoading ? (
              <div className="diary-loading">
                <div className="diary-loading-line" />
                <div className="diary-loading-line" />
                <div className="diary-loading-line short" />
              </div>
            ) : isEditing ? (
              <div className="diary-editor">
                {/* 心情选择 */}
                <div className="diary-editor-row">
                  <label className="diary-editor-label">心情</label>
                  <div className="mood-selector">
                    {MOODS.map(m => (
                      <button
                        key={m.emoji}
                        className={`mood-btn ${editMood === m.emoji ? 'active' : ''}`}
                        onClick={() => setEditMood(editMood === m.emoji ? '' : m.emoji)}
                        title={m.label}
                      >
                        {m.emoji}
                      </button>
                    ))}
                  </div>
                </div>

                {/* 天气选择 */}
                <div className="diary-editor-row">
                  <label className="diary-editor-label">天气</label>
                  <div className="weather-selector">
                    {WEATHERS.map(w => (
                      <button
                        key={w.icon}
                        className={`weather-btn ${editWeather === w.icon ? 'active' : ''}`}
                        onClick={() => setEditWeather(editWeather === w.icon ? '' : w.icon)}
                        title={w.label}
                      >
                        {w.icon} {w.label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* 编辑区 */}
                <div className="diary-editor-main">
                  <div className="diary-editor-tabs">
                    <button
                      className={`editor-tab ${!showPreview ? 'active' : ''}`}
                      onClick={() => setShowPreview(false)}
                    >
                      编辑
                    </button>
                    <button
                      className={`editor-tab ${showPreview ? 'active' : ''}`}
                      onClick={() => setShowPreview(true)}
                    >
                      预览
                    </button>
                  </div>

                  {showPreview ? (
                    <div
                      className="diary-preview markdown-body"
                      dangerouslySetInnerHTML={{ __html: renderMarkdown(editContent) }}
                    />
                  ) : (
                    <textarea
                      ref={textareaRef}
                      className="diary-textarea"
                      value={editContent}
                      onChange={e => setEditContent(e.target.value)}
                      placeholder="写下今天的日记...&#10;&#10;支持 Markdown 格式：&#10;* **加粗** *斜体* ~~删除线~~&#10;* 列表、标题、引用、代码块"
                      rows={16}
                    />
                  )}
                </div>

                {/* 操作按钮 */}
                <div className="diary-editor-actions">
                  <span className="diary-editor-hint">支持 Markdown 语法</span>
                  <div className="diary-editor-btns">
                    <button className="btn btn-cancel" onClick={handleCancelEdit}>取消</button>
                    <button
                      className="btn btn-save"
                      onClick={handleSave}
                      disabled={!editContent.trim() || saving}
                    >
                      {saving ? '保存中...' : '💾 保存日记'}
                    </button>
                  </div>
                </div>
              </div>
            ) : currentDiary ? (
              <div className="diary-view">
                {currentDiary.mood && (
                  <div className="diary-view-mood">
                    <span className="diary-view-mood-emoji">{currentDiary.mood}</span>
                    {currentDiary.weather && <span className="diary-view-weather">{currentDiary.weather}</span>}
                  </div>
                )}
                <div
                  className="diary-view-content markdown-body"
                  dangerouslySetInnerHTML={{ __html: renderMarkdown(currentDiary.content) }}
                />
                <div className="diary-view-footer">
                  <span className="diary-view-time">
                    更新于 {new Date(currentDiary.updatedAt).toLocaleString('zh-CN')}
                  </span>
                </div>
                <div className="diary-view-actions">
                  <button className="btn btn-edit" onClick={handleEdit}>✏️ 编辑</button>
                  <button className="btn btn-delete" onClick={handleDelete}>🗑️ 删除</button>
                </div>
              </div>
            ) : (
              <div className="diary-empty">
                <div className="diary-empty-icon">📝</div>
                <p className="diary-empty-text">今天还没有记录呢</p>
                <p className="diary-empty-sub">写一篇日记吧 ✍️</p>
                <button className="btn btn-write" onClick={handleStartWrite}>开始写日记</button>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}