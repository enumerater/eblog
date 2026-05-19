import { useEffect, useRef } from 'react';

/**
 * 鼠标悬浮光晕 — 为容器内的卡片添加跟随鼠标的柔和光晕
 * 用法：用 <MouseGlow> 包裹卡片列表即可
 */
export default function MouseGlow({ children, className = '', color = 'var(--accent)', size = 250 }) {
  const containerRef = useRef(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReduced) return;

    let rafId;

    const handleMouseMove = (e) => {
      if (rafId) cancelAnimationFrame(rafId);
      rafId = requestAnimationFrame(() => {
        const rect = container.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        container.style.setProperty('--glow-x', `${x}px`);
        container.style.setProperty('--glow-y', `${y}px`);
      });
    };

    const handleMouseLeave = () => {
      container.style.setProperty('--glow-opacity', '0');
    };

    const handleMouseEnter = () => {
      container.style.setProperty('--glow-opacity', '1');
    };

    container.addEventListener('mousemove', handleMouseMove);
    container.addEventListener('mouseleave', handleMouseLeave);
    container.addEventListener('mouseenter', handleMouseEnter);

    return () => {
      if (rafId) cancelAnimationFrame(rafId);
      container.removeEventListener('mousemove', handleMouseMove);
      container.removeEventListener('mouseleave', handleMouseLeave);
      container.removeEventListener('mouseenter', handleMouseEnter);
    };
  }, []);

  return (
    <div
      ref={containerRef}
      className={`mouse-glow-container ${className}`}
      style={{
        '--glow-color': color,
        '--glow-size': `${size}px`,
        '--glow-opacity': '0',
        '--glow-x': '0px',
        '--glow-y': '0px',
        position: 'relative',
      }}
    >
      <div
        className="mouse-glow-spot"
        style={{
          position: 'absolute',
          width: 'var(--glow-size)',
          height: 'var(--glow-size)',
          borderRadius: '50%',
          background: `radial-gradient(circle, var(--glow-color) 0%, transparent 70%)`,
          left: 'calc(var(--glow-x) - var(--glow-size) / 2)',
          top: 'calc(var(--glow-y) - var(--glow-size) / 2)',
          opacity: 'var(--glow-opacity)',
          transition: 'opacity 0.3s ease',
          pointerEvents: 'none',
          zIndex: 0,
        }}
      />
      <div style={{ position: 'relative', zIndex: 1 }}>
        {children}
      </div>
    </div>
  );
}