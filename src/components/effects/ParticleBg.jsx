import { useEffect, useRef } from 'react';

/**
 * 首页粒子背景 — 纯 Canvas 实现，零服务器开销
 * 粒子缓慢漂移，靠近时互连成线
 */
export default function ParticleBg({ color = '#c0392b', count = 60 }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    // 如果用户偏好减少动效，直接跳过
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReduced) return;

    const ctx = canvas.getContext('2d');
    let animId;
    let particles = [];
    let w, h;

    function resize() {
      const parent = canvas.parentElement;
      w = parent.offsetWidth;
      h = parent.offsetHeight;
      canvas.width = w * devicePixelRatio;
      canvas.height = h * devicePixelRatio;
      canvas.style.width = w + 'px';
      canvas.style.height = h + 'px';
      ctx.scale(devicePixelRatio, devicePixelRatio);
    }

    function createParticles() {
      particles = [];
      const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
      const baseColor = isDark ? '255,255,255' : '192,57,43';

      for (let i = 0; i < count; i++) {
        particles.push({
          x: Math.random() * w,
          y: Math.random() * h,
          vx: (Math.random() - 0.5) * 0.4,
          vy: (Math.random() - 0.5) * 0.4,
          r: Math.random() * 2 + 1.2,
          baseColor,
          alpha: Math.random() * 0.4 + 0.15,
        });
      }
    }

    function draw() {
      ctx.clearRect(0, 0, w, h);

      // 更新粒子位置
      for (const p of particles) {
        p.x += p.vx;
        p.y += p.vy;

        if (p.x < 0) p.x = w;
        if (p.x > w) p.x = 0;
        if (p.y < 0) p.y = h;
        if (p.y > h) p.y = 0;

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${p.baseColor},${p.alpha})`;
        ctx.fill();
      }

      // 画连线（距离近的粒子之间）
      const maxDist = 130;
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < maxDist) {
            const opacity = (1 - dist / maxDist) * 0.35;
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(${particles[i].baseColor},${opacity})`;
            ctx.lineWidth = 0.6;
            ctx.stroke();
          }
        }
      }

      animId = requestAnimationFrame(draw);
    }

    // 主题变化时重新创建粒子（颜色适配）
    const observer = new MutationObserver(() => {
      createParticles();
    });
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });

    resize();
    createParticles();
    draw();

    window.addEventListener('resize', () => {
      resize();
      createParticles();
    });

    return () => {
      cancelAnimationFrame(animId);
      observer.disconnect();
      window.removeEventListener('resize', resize);
    };
  }, [color, count]);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        pointerEvents: 'none',
        zIndex: 0,
      }}
    />
  );
}