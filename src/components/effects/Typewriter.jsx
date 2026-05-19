import { useState, useEffect } from 'react';

/**
 * 打字机效果 — 逐字显示文字，吃用户本地性能，不占服务器
 */
export default function Typewriter({ texts, speed = 80, pause = 2000, className = '' }) {
  const [displayed, setDisplayed] = useState('');
  const [textIndex, setTextIndex] = useState(0);
  const [charIndex, setCharIndex] = useState(0);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const currentText = texts[textIndex % texts.length];
    let timer;

    if (!deleting) {
      // 打字阶段
      if (charIndex < currentText.length) {
        timer = setTimeout(() => {
          setDisplayed(currentText.slice(0, charIndex + 1));
          setCharIndex(c => c + 1);
        }, speed);
      } else {
        // 打字完成，暂停后开始删除
        timer = setTimeout(() => setDeleting(true), pause);
      }
    } else {
      // 删除阶段
      if (charIndex > 0) {
        timer = setTimeout(() => {
          setDisplayed(currentText.slice(0, charIndex - 1));
          setCharIndex(c => c - 1);
        }, speed / 2);
      } else {
        // 删除完成，切换到下一句
        setDeleting(false);
        setTextIndex(i => i + 1);
        setCharIndex(0);
      }
    }

    return () => clearTimeout(timer);
  }, [textIndex, charIndex, deleting, texts, speed, pause]);

  return (
    <span className={className}>
      {displayed}
      <span className="typewriter-cursor">|</span>
    </span>
  );
}