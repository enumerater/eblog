import { useState, useEffect, useRef } from 'react';
import api, { auth } from '../api';
import './DanmakuOverlay.css';

const COLORS = ['#ff6b6b', '#ffd93d', '#6bcb77', '#4d96ff', '#ff6bb5', '#c084fc', '#fb923c'];
const FONT_SIZES = ['0.85rem', '0.95rem', '1rem', '1.05rem'];

export default function DanmakuOverlay({ articleId, visible }) {
  const [danmakuList, setDanmakuList] = useState([]);
  const lastIdRef = useRef(0);
  const timerRef = useRef(null);
  const containerRef = useRef(null);

  // ── 初始加载 + 轮询 ──
  useEffect(() => {
    if (!articleId || !visible) return;

    // 初始加载已有弹幕
    api.getDanmaku(articleId).then(list => {
      if (list && list.length > 0) {
        setDanmakuList(list);
        lastIdRef.current = list[list.length - 1].id;
      }
    }).catch(() => {});

    // 每 3s 增量拉取新弹幕
    timerRef.current = setInterval(async () => {
      try {
        const newList = await api.getDanmakuAfter(articleId, lastIdRef.current);
        if (newList && newList.length > 0) {
          setDanmakuList(prev => [...prev, ...newList]);
          lastIdRef.current = newList[newList.length - 1].id;
        }
      } catch { /* ignore */ }
    }, 3000);

    return () => {
      clearInterval(timerRef.current);
      setDanmakuList([]);
      lastIdRef.current = 0;
    };
  }, [articleId, visible]);

  // ── 自动删除已飘过的弹幕（保留最近 200 条） ──
  useEffect(() => {
    if (danmakuList.length > 200) {
      setDanmakuList(prev => prev.slice(-200));
    }
  }, [danmakuList.length]);

  const [inputValue, setInputValue] = useState('');
  const [sending, setSending] = useState(false);

  const handleSend = async (e) => {
    e.preventDefault();
    const text = inputValue.trim();
    if (!text || sending) return;

    setSending(true);
    try {
      const color = COLORS[Math.floor(Math.random() * COLORS.length)];
      const author = auth.getNickname() || '匿名';
      const data = await api.sendDanmaku({
        articleId,
        content: text,
        author,
        color,
        position: 0,
      });
      if (data) {
        setDanmakuList(prev => [...prev, data]);
        lastIdRef.current = data.id;
        setInputValue('');
      }
    } catch (err) {
      alert('发送失败: ' + err.message);
    } finally {
      setSending(false);
    }
  };

  if (!visible) return null;

  return (
    <div className="danmaku-section">
      <div className="danmaku-stage" ref={containerRef}>
        {danmakuList.map((d, i) => (
          <DanmakuItem key={d.id || i} danmaku={d} index={i} />
        ))}
      </div>
      <form className="danmaku-input-bar" onSubmit={handleSend}>
        <input
          value={inputValue}
          onChange={e => setInputValue(e.target.value)}
          placeholder="发送弹幕..."
          maxLength={200}
          className="danmaku-input"
        />
        <button type="submit" className="danmaku-send-btn" disabled={sending}>
          发送
        </button>
      </form>
    </div>
  );
}

function DanmakuItem({ danmaku, index }) {
  // 随机 delay 让弹幕错开
  const delay = (index % 10) * 0.8;
  const top = 10 + (index % 12) * 28;
  const fontSize = FONT_SIZES[index % FONT_SIZES.length];
  const speed = 8 + (index % 5) * 2;

  return (
    <div
      className="danmaku-item"
      style={{
        color: danmaku.color || '#fff',
        top: `${top}px`,
        animationDelay: `${delay}s`,
        animationDuration: `${speed}s`,
        fontSize,
      }}
    >
      <span className="danmaku-author">[{danmaku.author}]</span>
      {danmaku.content}
    </div>
  );
}