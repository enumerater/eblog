import { useEffect, useRef } from 'react';

/**
 * 阅读进度条 — 文章详情页顶部，显示当前滚动阅读进度
 */
export default function ReadingProgress() {
  const barRef = useRef(null);

  useEffect(() => {
    const onScroll = () => {
      const scrollTop = window.scrollY;
      const docHeight = document.documentElement.scrollHeight - window.innerHeight;
      const progress = docHeight > 0 ? Math.min(scrollTop / docHeight, 1) : 0;
      if (barRef.current) {
        barRef.current.style.transform = `scaleX(${progress})`;
      }
    };

    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <div className="reading-progress-track">
      <div ref={barRef} className="reading-progress-bar" />
    </div>
  );
}