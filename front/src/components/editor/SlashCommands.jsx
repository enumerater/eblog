import { Extension } from '@tiptap/core';
import Suggestion from '@tiptap/suggestion';
import { ReactRenderer } from '@tiptap/react';
import { useEffect, useState, useRef, useCallback, forwardRef, useImperativeHandle } from 'react';
import tippy from 'tippy.js';
import 'tippy.js/dist/tippy.css';

// --- SVG Icons (inline, no dependency) ---
const cmdIcons = {
  h1: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M17 12l3-2v8"/></svg>,
  h2: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M21 18h-4c0-4 4-3 4-6 0-1.5-2-2.5-4-1"/></svg>,
  h3: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M17.5 10.5c1.7-1 3.5 0 3.5 1.5a2 2 0 0 1-2 2"/><path d="M17 17.5c2 1.5 4 .3 4-1.5a2 2 0 0 0-2-2"/></svg>,
  paragraph: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M13 4v16"/><path d="M17 4v16"/><path d="M13 4H9a4 4 0 0 0 0 8h4"/></svg>,
  bulletList: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><circle cx="3.5" cy="6" r="1.5" fill="currentColor" stroke="none"/><circle cx="3.5" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="3.5" cy="18" r="1.5" fill="currentColor" stroke="none"/></svg>,
  orderedList: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="10" y1="6" x2="21" y2="6"/><line x1="10" y1="12" x2="21" y2="12"/><line x1="10" y1="18" x2="21" y2="18"/><text x="2" y="8" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">1</text><text x="2" y="14" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">2</text><text x="2" y="20" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">3</text></svg>,
  blockquote: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3z"/></svg>,
  codeBlock: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/><line x1="12" y1="2" x2="12" y2="22"/></svg>,
  table: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>,
  hr: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="2" y1="12" x2="22" y2="12"/></svg>,
  image: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>,
};

// --- Command definitions ---
const COMMANDS = [
  { title: '标题 1', description: '大标题', icon: cmdIcons.h1, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).setNode('heading', { level: 1 }).run(); } },
  { title: '标题 2', description: '中标题', icon: cmdIcons.h2, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).setNode('heading', { level: 2 }).run(); } },
  { title: '标题 3', description: '小标题', icon: cmdIcons.h3, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).setNode('heading', { level: 3 }).run(); } },
  { title: '正文', description: '普通段落文本', icon: cmdIcons.paragraph, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).setNode('paragraph').run(); } },
  { title: '无序列表', description: '创建无序列表', icon: cmdIcons.bulletList, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).toggleBulletList().run(); } },
  { title: '有序列表', description: '创建有序列表', icon: cmdIcons.orderedList, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).toggleOrderedList().run(); } },
  { title: '引用', description: '创建引用块', icon: cmdIcons.blockquote, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).toggleBlockquote().run(); } },
  { title: '代码块', description: '插入代码块', icon: cmdIcons.codeBlock, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).toggleCodeBlock().run(); } },
  { title: '表格', description: '插入 3x3 表格', icon: cmdIcons.table, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(); } },
  { title: '分割线', description: '插入水平分割线', icon: cmdIcons.hr, command: ({ editor, range }) => { editor.chain().focus().deleteRange(range).setHorizontalRule().run(); } },
  { title: '图片', description: '插入网络图片', icon: cmdIcons.image, command: ({ editor, range }) => {
    const url = window.prompt('输入图片链接');
    if (url) editor.chain().focus().deleteRange(range).setImage({ src: url }).run();
  }},
];

// --- React popup component ---
const SlashCommandList = forwardRef(function SlashCommandList(props, ref) {
  const { items, command } = props;
  const [selectedIndex, setSelectedIndex] = useState(0);
  const listRef = useRef(null);

  useEffect(() => { setSelectedIndex(0); }, [items]);

  useEffect(() => {
    const el = listRef.current?.children[selectedIndex];
    el?.scrollIntoView({ block: 'nearest' });
  }, [selectedIndex]);

  const handleSelect = useCallback((item) => { command(item); }, [command]);

  useImperativeHandle(ref, () => ({
    onKeyDown: ({ event }) => {
      if (event.key === 'ArrowUp') {
        setSelectedIndex((prev) => (prev + items.length - 1) % items.length);
        return true;
      }
      if (event.key === 'ArrowDown') {
        setSelectedIndex((prev) => (prev + 1) % items.length);
        return true;
      }
      if (event.key === 'Enter') {
        handleSelect(items[selectedIndex]);
        return true;
      }
      return false;
    },
  }), [items, selectedIndex, handleSelect]);

  if (items.length === 0) {
    return <div className="slash-menu-empty">没有匹配的命令</div>;
  }

  return (
    <div className="slash-menu" ref={listRef}>
      {items.map((item, index) => (
        <button
          key={item.title}
          type="button"
          className={`slash-menu-item${index === selectedIndex ? ' is-selected' : ''}`}
          onClick={() => handleSelect(item)}
          onMouseEnter={() => setSelectedIndex(index)}
        >
          <span className="slash-menu-icon">{item.icon}</span>
          <span className="slash-menu-text">
            <span className="slash-menu-title">{item.title}</span>
            <span className="slash-menu-desc">{item.description}</span>
          </span>
        </button>
      ))}
    </div>
  );
});

// --- Suggestion config ---
function getSuggestion() {
  return {
    char: '/',
    allowedPrefixes: null,
    startOfLine: false,
    allowSpaces: false,
    items: ({ query }) => {
      const q = query.toLowerCase();
      return COMMANDS.filter(item =>
        item.title.toLowerCase().includes(q) ||
        item.description.toLowerCase().includes(q)
      ).slice(0, 10);
    },
    render: () => {
      let component;
      let popup;
      return {
        onStart: (props) => {
          component = new ReactRenderer(SlashCommandList, { props, editor: props.editor });
          if (!props.clientRect) return;
          popup = tippy('body', {
            getReferenceClientRect: props.clientRect,
            appendTo: () => document.body,
            content: component.element,
            showOnCreate: true,
            interactive: true,
            trigger: 'manual',
            placement: 'bottom-start',
            animation: false,
          });
        },
        onUpdate: (props) => {
          component?.updateProps(props);
          if (!props.clientRect) return;
          popup?.[0]?.setProps({ getReferenceClientRect: props.clientRect });
        },
        onKeyDown: (props) => {
          if (props.event.key === 'Escape') {
            popup?.[0]?.hide();
            return true;
          }
          return component?.ref?.onKeyDown(props) ?? false;
        },
        onExit: () => {
          popup?.[0]?.destroy();
          component?.destroy();
        },
      };
    },
  };
}

// --- Tiptap Extension ---
export default Extension.create({
  name: 'slashCommands',
  addOptions() {
    return { suggestion: { char: '/', command: ({ editor, range, props }) => { props.command({ editor, range }); } } };
  },
  addProseMirrorPlugins() {
    return [Suggestion({ editor: this.editor, ...this.options.suggestion, ...getSuggestion() })];
  },
});
