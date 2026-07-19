import { useCallback, useRef, useState, useEffect } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import { Table } from '@tiptap/extension-table';
import TableRow from '@tiptap/extension-table-row';
import TableCell from '@tiptap/extension-table-cell';
import TableHeader from '@tiptap/extension-table-header';
import TextAlign from '@tiptap/extension-text-align';
import Underline from '@tiptap/extension-underline';
import Highlight from '@tiptap/extension-highlight';
import { TextStyle } from '@tiptap/extension-text-style';
import FontFamily from '@tiptap/extension-font-family';
import Color from '@tiptap/extension-color';
import TaskList from '@tiptap/extension-task-list';
import TaskItem from '@tiptap/extension-task-item';
import Subscript from '@tiptap/extension-subscript';
import Superscript from '@tiptap/extension-superscript';
import CharacterCount from '@tiptap/extension-character-count';
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight';
import { common, createLowlight } from 'lowlight';
import { marked } from 'marked';
import FontSize from './extensions/FontSize';
import codeBlockNodeView from './CodeBlockView';
import EditorBubbleMenu from './BubbleMenu';
import SlashCommands from './SlashCommands';
import './RichEditor.css';

const lowlight = createLowlight(common);

const CustomCodeBlock = CodeBlockLowlight.extend({
  addNodeView() {
    return codeBlockNodeView;
  },
}).configure({
  lowlight,
  defaultLanguage: 'javascript',
  enableTabIndentation: true,
  tabSize: 4,
});

// --- SVG Icons ---
const I = {
  bold: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/><path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/></svg>,
  italic: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="19" y1="4" x2="10" y2="4"/><line x1="14" y1="20" x2="5" y2="20"/><line x1="15" y1="4" x2="9" y2="20"/></svg>,
  underline: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M6 3v7a6 6 0 0 0 6 6 6 6 0 0 0 6-6V3"/><line x1="4" y1="21" x2="20" y2="21"/></svg>,
  strikethrough: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M17.3 4.9c-2.3-.6-4.4-1-6.2-.9-2.7 0-5.3.7-5.3 3.6 0 1.5 1.8 3.3 7.2 3.3"/><path d="M13 15.1c1.3.3 2.5.9 2.5 2.5 0 2.5-3.5 3.5-6 3.5-1.5 0-3.1-.3-4.5-.7"/><line x1="2" y1="12" x2="22" y2="12"/></svg>,
  highlight: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="m9 11-6 6v3h9l3-3"/><path d="m22 12-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4"/></svg>,
  inlineCode: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>,
  h1: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M17 12l3-2v8"/></svg>,
  h2: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M21 18h-4c0-4 4-3 4-6 0-1.5-2-2.5-4-1"/></svg>,
  h3: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M17.5 10.5c1.7-1 3.5 0 3.5 1.5a2 2 0 0 1-2 2"/><path d="M17 17.5c2 1.5 4 .3 4-1.5a2 2 0 0 0-2-2"/></svg>,
  h4: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M18 18v-6h-4"/><path d="M18 18h4"/><path d="M14 14h6"/></svg>,
  h5: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M18 12h-4v6h4"/><path d="M14 15h4"/></svg>,
  h6: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M19 18c-1.5 0-3-1.5-3-3.5s1-3.5 3-3.5 2.5 1.5 2.5 3.5"/><path d="M18.5 18c1.5 0 3-1 3-3"/></svg>,
  alignLeft: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="17" y1="10" x2="3" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/><line x1="21" y1="14" x2="3" y2="14"/><line x1="17" y1="18" x2="3" y2="18"/></svg>,
  alignCenter: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="10" x2="6" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/><line x1="21" y1="14" x2="3" y2="14"/><line x1="18" y1="18" x2="6" y2="18"/></svg>,
  alignRight: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="21" y1="10" x2="7" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/><line x1="21" y1="14" x2="3" y2="14"/><line x1="21" y1="18" x2="7" y2="18"/></svg>,
  bulletList: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><circle cx="3.5" cy="6" r="1.5" fill="currentColor" stroke="none"/><circle cx="3.5" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="3.5" cy="18" r="1.5" fill="currentColor" stroke="none"/></svg>,
  orderedList: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="10" y1="6" x2="21" y2="6"/><line x1="10" y1="12" x2="21" y2="12"/><line x1="10" y1="18" x2="21" y2="18"/><text x="2" y="8" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">1</text><text x="2" y="14" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">2</text><text x="2" y="20" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">3</text></svg>,
  taskList: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>,
  blockquote: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3z"/></svg>,
  codeBlock: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>,
  hr: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="2" y1="12" x2="22" y2="12"/></svg>,
  table: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>,
  image: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>,
  import: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>,
  undo: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>,
  redo: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>,
  subscript: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M4 20l8-16"/><path d="M12 20l8-16"/><path d="M18 20h4v-2c0-1-.5-1.5-1-2h2"/></svg>,
  superscript: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M4 20l8-16"/><path d="M12 20l8-16"/><path d="M18 4h4v2c0 1-.5 1.5-1 2h2"/></svg>,
  fullscreen: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>,
  fullscreenExit: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="4 14 10 14 10 20"/><polyline points="20 10 14 10 14 4"/><line x1="14" y1="10" x2="21" y2="3"/><line x1="3" y1="21" x2="10" y2="14"/></svg>,
  link: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>,
  tableInsertColBefore: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="12" y1="3" x2="12" y2="21"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="12" x2="21" y2="12"/></svg>,
  tableInsertColAfter: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="12" y1="3" x2="12" y2="21"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="12" x2="21" y2="12"/></svg>,
  tableDeleteTable: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>,
};

// Font size options
const FONT_SIZES = [
  { label: '默认', value: '' },
  { label: '12px', value: '12px' },
  { label: '14px', value: '14px' },
  { label: '16px', value: '16px' },
  { label: '18px', value: '18px' },
  { label: '20px', value: '20px' },
  { label: '24px', value: '24px' },
  { label: '28px', value: '28px' },
  { label: '32px', value: '32px' },
  { label: '36px', value: '36px' },
];

// Font family options
const FONT_FAMILIES = [
  { label: '默认', value: '' },
  { label: '宋体', value: 'SimSun, serif' },
  { label: '黑体', value: 'SimHei, sans-serif' },
  { label: '楷体', value: 'KaiTi, serif' },
  { label: '仿宋', value: 'FangSong, serif' },
  { label: '等宽', value: 'monospace' },
  { label: '微软雅黑', value: '"Microsoft YaHei", sans-serif' },
  { label: '思源黑体', value: '"Source Han Sans SC", sans-serif' },
];

// Preset colors
const COLORS = [
  '#000000', '#434343', '#666666', '#999999', '#b7b7b7', '#cccccc', '#e0e0e0', '#ffffff',
  '#980000', '#ff0000', '#ff9900', '#ffff00', '#00ff00', '#00ffff', '#4a86e8', '#0000ff',
  '#9900ff', '#ff00ff', '#e6b8af', '#f4cccc', '#fce5cd', '#fff2cc', '#d9ead3', '#d0e0e3',
  '#c9daf8', '#cfe2f3', '#d9d2e9', '#ead1dc',
];

function looksLikeMarkdown(text) {
  const lines = text.split('\n');

  // If it looks like code (indented, high special char density), skip
  let codeLines = 0;
  let totalNonEmpty = 0;
  for (const line of lines) {
    const t = line.trim();
    if (!t) continue;
    totalNonEmpty++;
    // Lines starting with # followed by non-space chars are likely code comments, not headings
    if (/^#\S/.test(t)) { codeLines++; continue; }
    if (/^\s{2,}/.test(line)) { codeLines++; continue; }
    // High density of special chars suggests code
    const special = (t.match(/[{}[\]();:.,=+\-*/%&|^~!<>@#$]/g) || []).length;
    if (special > 3 || (t.length > 0 && special / t.length > 0.15)) { codeLines++; }
  }
  if (totalNonEmpty > 0 && codeLines / totalNonEmpty > 0.5) return false;

  let score = 0;
  for (const line of lines) {
    const t = line.trim();
    if (!t) continue;
    if (/^#{1,6}\s/.test(t)) score += 3;
    if (/^```/.test(t)) score += 3;
    if (/^>\s?/.test(t)) score += 2;
    if (/^[-*+]\s/.test(t)) score += 1;
    if (/^\d+\.\s/.test(t)) score += 1;
    if (/^\|/.test(t)) score += 2;
    if (/^-{3,}$/.test(t) || /^\*{3,}$/.test(t)) score += 2;
    if (/\*\*[^*]+\*\*/.test(t)) score += 2;
    if (/__[^_]+__/.test(t)) score += 2;
    if (/`[^`\n]+`/.test(t)) score += 1;
    if (/\[.+?\]\(.+?\)/.test(t)) score += 2;
    if (/!\[.*?\]\(.+?\)/.test(t)) score += 2;
    if (/~~[^~]+~~/.test(t)) score += 1;
  }
  return score >= 2;
}

function markdownToHtml(md) {
  return marked.parse(md, { breaks: true, gfm: true });
}

// --- Link Dialog Component ---
function LinkDialog({ editor, onClose }) {
  const [url, setUrl] = useState('');
  const [text, setText] = useState('');
  const inputRef = useRef(null);

  useEffect(() => {
    if (editor) {
      const previousUrl = editor.getAttributes('link').href || '';
      setUrl(previousUrl || 'https://');
      const { from, to } = editor.state.selection;
      const selectedText = editor.state.doc.textBetween(from, to);
      setText(selectedText);
    }
    setTimeout(() => inputRef.current?.focus(), 50);
  }, [editor]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!editor) return;
    if (url.trim() === '') {
      editor.chain().focus().extendMarkRange('link').unsetLink().run();
    } else {
      editor.chain().focus().extendMarkRange('link').setLink({ href: url.trim() }).run();
    }
    onClose();
  };

  const handleRemove = () => {
    if (!editor) return;
    editor.chain().focus().extendMarkRange('link').unsetLink().run();
    onClose();
  };

  return (
    <div className="link-dialog-overlay" onClick={onClose}>
      <div className="link-dialog" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="link-dialog-field">
            <label>链接地址</label>
            <input
              ref={inputRef}
              type="url"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://..."
            />
          </div>
          <div className="link-dialog-field">
            <label>显示文字</label>
            <input
              type="text"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="链接文字"
            />
          </div>
          <div className="link-dialog-actions">
            <button type="submit" className="link-dialog-btn link-dialog-btn-primary">确定</button>
            <button type="button" className="link-dialog-btn link-dialog-btn-danger" onClick={handleRemove}>移除链接</button>
            <button type="button" className="link-dialog-btn" onClick={onClose}>取消</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// --- Color Picker Component ---
function ColorPicker({ editor, onClose, type }) {
  const handleColor = (color) => {
    if (!editor) return;
    if (type === 'text') {
      editor.chain().focus().setColor(color).run();
    } else {
      editor.chain().focus().toggleHighlight({ color }).run();
    }
    onClose?.();
  };

  const handleCustom = () => {
    const color = window.prompt('输入颜色值 (如 #ff0000, red, rgb(255,0,0))');
    if (color && editor) {
      if (type === 'text') {
        editor.chain().focus().setColor(color).run();
      } else {
        editor.chain().focus().toggleHighlight({ color }).run();
      }
    }
    onClose?.();
  };

  return (
    <div className="color-picker-overlay" onClick={onClose}>
      <div className="color-picker" onClick={(e) => e.stopPropagation()}>
        <div className="color-picker-grid">
          {COLORS.map((c) => (
            <button
              key={c}
              type="button"
              className="color-picker-swatch"
              style={{ background: c, border: c === '#ffffff' ? '1px solid #ddd' : 'none' }}
              onClick={() => handleColor(c)}
              title={c}
            />
          ))}
        </div>
        <button type="button" className="color-picker-custom" onClick={handleCustom}>
          自定义颜色...
        </button>
        <button type="button" className="color-picker-custom" onClick={() => { type === 'text' ? editor?.chain().focus().unsetColor().run() : editor?.chain().focus().unsetHighlight().run(); onClose?.(); }}>
          清除颜色
        </button>
      </div>
    </div>
  );
}

export default function RichEditor({ content, onChange, onUploadImage }) {
  const fileInputRef = useRef(null);
  const imageInputRef = useRef(null);
  const [uploadingImages, setUploadingImages] = useState(false);
  const [showLinkDialog, setShowLinkDialog] = useState(false);
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [showBgColorPicker, setShowBgColorPicker] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showHeadingMenu, setShowHeadingMenu] = useState(false);
  const [showTableMenu, setShowTableMenu] = useState(false);
  const headingMenuRef = useRef(null);
  const tableMenuRef = useRef(null);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({ codeBlock: false, heading: { levels: [1, 2, 3, 4, 5, 6] } }),
      CustomCodeBlock,
      Underline,
      Highlight.configure({ multicolor: true }),
      TextStyle,
      FontFamily,
      Color,
      FontSize,
      Subscript,
      Superscript,
      TaskList,
      TaskItem.configure({ nested: true }),
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      Image.configure({ inline: true }),
      Link.configure({
        openOnClick: false,
        HTMLAttributes: { rel: 'noopener noreferrer', target: '_blank' },
      }),
      Placeholder.configure({ placeholder: '开始写文章... (输入 / 唤出命令菜单，支持粘贴 Markdown)' }),
      Table.configure({ resizable: true }),
      TableRow,
      TableCell,
      TableHeader,
      CharacterCount,
      SlashCommands,
    ],
    content: content || '',
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML());
    },
  });

  // Close heading/table dropdown on outside click
  useEffect(() => {
    const handleClick = (e) => {
      if (headingMenuRef.current && !headingMenuRef.current.contains(e.target)) {
        setShowHeadingMenu(false);
      }
      if (tableMenuRef.current && !tableMenuRef.current.contains(e.target)) {
        setShowTableMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // Upload handlers
  const uploadAndInsertImage = useCallback(async (file) => {
    if (!editor || !onUploadImage) return;
    setUploadingImages(true);
    try {
      const url = await onUploadImage(file);
      editor.chain().focus().setImage({ src: url }).run();
    } catch (err) {
      alert('图片上传失败: ' + (err.message || '未知错误'));
    } finally {
      setUploadingImages(false);
    }
  }, [editor, onUploadImage]);

  const handleImageFileChange = useCallback((e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    uploadAndInsertImage(file);
    e.target.value = '';
  }, [uploadAndInsertImage]);

  // Capture-phase paste: intercept before ProseMirror
  useEffect(() => {
    if (!editor) return;

    const handlePaste = (event) => {
      // Check for image files first
      const imageFiles = Array.from(event.clipboardData?.files || []).filter(f => f.type.startsWith('image/'));
      if (imageFiles.length > 0) {
        event.preventDefault();
        event.stopPropagation();
        imageFiles.forEach(file => uploadAndInsertImage(file));
        return;
      }

      const text = event.clipboardData?.getData('text/plain');
      if (!text || !text.trim()) return;
      if (!looksLikeMarkdown(text)) return;

      event.preventDefault();
      event.stopPropagation();

      const html = markdownToHtml(text);
      const view = editor.view;
      const { from, to } = view.state.selection;

      requestAnimationFrame(() => {
        editor.chain()
          .focus()
          .setTextSelection({ from, to })
          .insertContent(html)
          .run();
      });
    };

    const editorEl = editor.view.dom;
    editorEl.addEventListener('paste', handlePaste, true);
    return () => editorEl.removeEventListener('paste', handlePaste, true);
  }, [editor, uploadAndInsertImage]);

  // Drop .md files and images
  useEffect(() => {
    if (!editor) return;

    const handleDrop = (event) => {
      const files = event.dataTransfer?.files;
      if (!files?.length) return;

      const imgFile = Array.from(files).find(f => f.type.startsWith('image/'));
      if (imgFile) {
        event.preventDefault();
        event.stopPropagation();
        uploadAndInsertImage(imgFile);
        return;
      }

      const mdFile = Array.from(files).find(f =>
        f.name.endsWith('.md') || f.name.endsWith('.markdown') || f.type === 'text/markdown'
      );
      if (!mdFile) return;

      event.preventDefault();
      event.stopPropagation();
      const reader = new FileReader();
      reader.onload = (e) => {
        const html = markdownToHtml(e.target.result);
        editor.commands.setContent(html);
      };
      reader.readAsText(mdFile);
    };

    const handleDragOver = (e) => {
      if (e.dataTransfer?.types?.includes('Files')) {
        e.preventDefault();
      }
    };

    const editorEl = editor.view.dom;
    editorEl.addEventListener('drop', handleDrop, true);
    editorEl.addEventListener('dragover', handleDragOver);
    return () => {
      editorEl.removeEventListener('drop', handleDrop, true);
      editorEl.removeEventListener('dragover', handleDragOver);
    };
  }, [editor, uploadAndInsertImage]);

  const handleImportMd = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const handleFileChange = useCallback((e) => {
    const file = e.target.files?.[0];
    if (!file || !editor) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const html = markdownToHtml(ev.target.result);
      editor.commands.setContent(html);
    };
    reader.readAsText(file);
    e.target.value = '';
  }, [editor]);

  const toggleFullscreen = useCallback(() => {
    setIsFullscreen(prev => !prev);
  }, []);

  // Table control helpers
  const addColumnBefore = () => editor?.chain().focus().addColumnBefore().run();
  const addColumnAfter = () => editor?.chain().focus().addColumnAfter().run();
  const addRowBefore = () => editor?.chain().focus().addRowBefore().run();
  const addRowAfter = () => editor?.chain().focus().addRowAfter().run();
  const deleteColumn = () => editor?.chain().focus().deleteColumn().run();
  const deleteRow = () => editor?.chain().focus().deleteRow().run();
  const deleteTable = () => editor?.chain().focus().deleteTable().run();

  if (!editor) return null;

  const isInTable = editor.isActive('table');
  const isTableSelected = isInTable;

  const ToolBtn = ({ onClick, active, title, children, ...rest }) => (
    <button
      type="button"
      onClick={onClick}
      className={`toolbar-btn ${active ? 'active' : ''}`}
      title={title}
      {...rest}
    >
      {children}
    </button>
  );

  const currentFontSize = editor.getAttributes('textStyle').fontSize || '';
  const currentFontFamily = editor.getAttributes('textStyle').fontFamily || '';

  // Character count
  const charCount = editor.storage.characterCount?.characters?.() || 0;
  const wordCount = editor.storage.characterCount?.words?.() || 0;
  const readingTime = Math.max(1, Math.ceil(wordCount / 200));

  return (
    <div className={`rich-editor${isFullscreen ? ' fullscreen' : ''}`}>
      <input
        ref={fileInputRef}
        type="file"
        accept=".md,.markdown"
        style={{ display: 'none' }}
        onChange={handleFileChange}
      />
      <input
        ref={imageInputRef}
        type="file"
        accept="image/*"
        style={{ display: 'none' }}
        onChange={handleImageFileChange}
      />

      <div className="toolbar">
        {/* Format group */}
        <div className="toolbar-group">
          <ToolBtn onClick={() => editor.chain().focus().toggleBold().run()} active={editor.isActive('bold')} title="粗体 (Ctrl+B)">
            {I.bold}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleItalic().run()} active={editor.isActive('italic')} title="斜体 (Ctrl+I)">
            {I.italic}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleUnderline().run()} active={editor.isActive('underline')} title="下划线 (Ctrl+U)">
            {I.underline}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleStrike().run()} active={editor.isActive('strike')} title="删除线">
            {I.strikethrough}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleCode().run()} active={editor.isActive('code')} title="行内代码">
            {I.inlineCode}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleHighlight().run()} active={editor.isActive('highlight')} title="高亮">
            {I.highlight}
          </ToolBtn>
        </div>

        <div className="toolbar-divider" />

        {/* Subscript / Superscript */}
        <div className="toolbar-group">
          <ToolBtn onClick={() => editor.chain().focus().toggleSubscript().run()} active={editor.isActive('subscript')} title="下标">
            {I.subscript}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleSuperscript().run()} active={editor.isActive('superscript')} title="上标">
            {I.superscript}
          </ToolBtn>
        </div>

        <div className="toolbar-divider" />

        {/* Heading dropdown */}
        <div className="toolbar-group" style={{ position: 'relative' }} ref={headingMenuRef}>
          <button
            type="button"
            className="toolbar-btn toolbar-select"
            onClick={() => setShowHeadingMenu(!showHeadingMenu)}
            title="标题"
          >
            {editor.isActive('heading', { level: 1 }) ? 'H1' :
             editor.isActive('heading', { level: 2 }) ? 'H2' :
             editor.isActive('heading', { level: 3 }) ? 'H3' :
             editor.isActive('heading', { level: 4 }) ? 'H4' :
             editor.isActive('heading', { level: 5 }) ? 'H5' :
             editor.isActive('heading', { level: 6 }) ? 'H6' : '正文'}
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="6 9 12 15 18 9"/></svg>
          </button>
          {showHeadingMenu && (
            <div className="toolbar-dropdown">
              <button type="button" className="toolbar-dropdown-item" onClick={() => { editor.chain().focus().setParagraph().run(); setShowHeadingMenu(false); }}>
                正文
              </button>
              {[1, 2, 3, 4, 5, 6].map(level => (
                <button
                  key={level}
                  type="button"
                  className={`toolbar-dropdown-item${editor.isActive('heading', { level }) ? ' active' : ''}`}
                  onClick={() => { editor.chain().focus().toggleHeading({ level }).run(); setShowHeadingMenu(false); }}
                >
                  H{level}
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="toolbar-divider" />

        {/* Font size */}
        <div className="toolbar-group">
          <select
            className="toolbar-select toolbar-select-small"
            value={currentFontSize}
            onChange={(e) => {
              const val = e.target.value;
              if (val) {
                editor.chain().focus().setFontSize(val).run();
              } else {
                editor.chain().focus().unsetFontSize().run();
              }
            }}
            title="字号"
          >
            {FONT_SIZES.map(fs => (
              <option key={fs.value} value={fs.value}>{fs.label}</option>
            ))}
          </select>
        </div>

        {/* Font family */}
        <div className="toolbar-group">
          <select
            className="toolbar-select toolbar-select-small"
            value={currentFontFamily}
            onChange={(e) => {
              const val = e.target.value;
              if (val) {
                editor.chain().focus().setFontFamily(val).run();
              } else {
                editor.chain().focus().unsetFontFamily().run();
              }
            }}
            title="字体"
          >
            {FONT_FAMILIES.map(ff => (
              <option key={ff.value} value={ff.value}>{ff.label}</option>
            ))}
          </select>
        </div>

        <div className="toolbar-divider" />

        {/* Text color */}
        <div className="toolbar-group" style={{ position: 'relative' }}>
          <ToolBtn
            onClick={() => setShowColorPicker(!showColorPicker)}
            active={!!editor.getAttributes('textStyle').color}
            title="文字颜色"
          >
            <span style={{ position: 'relative' }}>
              {I.highlight}
              <span style={{
                position: 'absolute', bottom: -2, left: '50%', transform: 'translateX(-50%)',
                width: 12, height: 3, borderRadius: 1,
                background: editor.getAttributes('textStyle').color || 'currentColor',
              }} />
            </span>
          </ToolBtn>
          {showColorPicker && <ColorPicker editor={editor} onClose={() => setShowColorPicker(false)} type="text" />}
        </div>

        {/* Background color */}
        <div className="toolbar-group" style={{ position: 'relative' }}>
          <ToolBtn
            onClick={() => setShowBgColorPicker(!showBgColorPicker)}
            active={editor.isActive('highlight')}
            title="背景色"
          >
            <span style={{ position: 'relative' }}>
              {I.highlight}
              <span style={{
                position: 'absolute', bottom: -2, left: '50%', transform: 'translateX(-50%)',
                width: 14, height: 3, borderRadius: 1, background: '#ffd700',
              }} />
            </span>
          </ToolBtn>
          {showBgColorPicker && <ColorPicker editor={editor} onClose={() => setShowBgColorPicker(false)} type="bg" />}
        </div>

        <div className="toolbar-divider" />

        {/* Alignment */}
        <div className="toolbar-group">
          <ToolBtn onClick={() => editor.chain().focus().setTextAlign('left').run()} active={editor.isActive({ textAlign: 'left' })} title="左对齐">
            {I.alignLeft}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().setTextAlign('center').run()} active={editor.isActive({ textAlign: 'center' })} title="居中">
            {I.alignCenter}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().setTextAlign('right').run()} active={editor.isActive({ textAlign: 'right' })} title="右对齐">
            {I.alignRight}
          </ToolBtn>
        </div>

        <div className="toolbar-divider" />

        {/* Lists & Block */}
        <div className="toolbar-group">
          <ToolBtn onClick={() => editor.chain().focus().toggleBulletList().run()} active={editor.isActive('bulletList')} title="无序列表">
            {I.bulletList}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleOrderedList().run()} active={editor.isActive('orderedList')} title="有序列表">
            {I.orderedList}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleTaskList().run()} active={editor.isActive('taskList')} title="任务列表">
            {I.taskList}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleBlockquote().run()} active={editor.isActive('blockquote')} title="引用">
            {I.blockquote}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleCodeBlock().run()} active={editor.isActive('codeBlock')} title="代码块">
            {I.codeBlock}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().setHorizontalRule().run()} title="分割线">
            {I.hr}
          </ToolBtn>
        </div>

        <div className="toolbar-divider" />

        {/* Table controls */}
        <div className="toolbar-group" style={{ position: 'relative' }} ref={tableMenuRef}>
          <ToolBtn
            onClick={() => {
              if (isTableSelected) {
                setShowTableMenu(!showTableMenu);
              } else {
                editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();
              }
            }}
            active={isTableSelected}
            title={isTableSelected ? '表格操作' : '插入表格'}
          >
            {I.table}
          </ToolBtn>
          {showTableMenu && isTableSelected && (
            <div className="toolbar-dropdown">
              <button type="button" className="toolbar-dropdown-item" onClick={() => { addColumnBefore(); setShowTableMenu(false); }}>左侧插入列</button>
              <button type="button" className="toolbar-dropdown-item" onClick={() => { addColumnAfter(); setShowTableMenu(false); }}>右侧插入列</button>
              <button type="button" className="toolbar-dropdown-item" onClick={() => { addRowBefore(); setShowTableMenu(false); }}>上方插入行</button>
              <button type="button" className="toolbar-dropdown-item" onClick={() => { addRowAfter(); setShowTableMenu(false); }}>下方插入行</button>
              <div className="toolbar-dropdown-divider" />
              <button type="button" className="toolbar-dropdown-item" onClick={() => { deleteColumn(); setShowTableMenu(false); }}>删除列</button>
              <button type="button" className="toolbar-dropdown-item" onClick={() => { deleteRow(); setShowTableMenu(false); }}>删除行</button>
              <div className="toolbar-dropdown-divider" />
              <button type="button" className="toolbar-dropdown-item toolbar-dropdown-item-danger" onClick={() => { deleteTable(); setShowTableMenu(false); }}>删除表格</button>
            </div>
          )}
        </div>

        {/* Image & Link */}
        <div className="toolbar-group">
          <ToolBtn
            onClick={() => imageInputRef.current?.click()}
            onContextMenu={(e) => {
              e.preventDefault();
              const url = window.prompt('输入图片链接');
              if (url) editor.chain().focus().setImage({ src: url }).run();
            }}
            title="左键上传图片，右键输入URL"
          >
            {I.image}
            {uploadingImages && <span className="toolbar-uploading" />}
          </ToolBtn>
          <ToolBtn
            onClick={() => setShowLinkDialog(true)}
            active={editor.isActive('link')}
            title="链接"
          >
            {I.link}
          </ToolBtn>
        </div>

        <div className="toolbar-divider" />

        {/* Tools */}
        <div className="toolbar-group">
          <ToolBtn onClick={handleImportMd} title="导入 Markdown 文件">
            {I.import}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().undo().run()} title="撤销 (Ctrl+Z)">
            {I.undo}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().redo().run()} title="重做 (Ctrl+Y)">
            {I.redo}
          </ToolBtn>
          <ToolBtn onClick={toggleFullscreen} title={isFullscreen ? '退出全屏' : '全屏编辑'}>
            {isFullscreen ? I.fullscreenExit : I.fullscreen}
          </ToolBtn>
        </div>
      </div>

      <EditorBubbleMenu editor={editor} />
      <EditorContent editor={editor} className="editor-content" />

      {/* Status bar */}
      <div className="editor-statusbar">
        <span className="editor-statusbar-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
          {wordCount} 字 · {charCount} 字符
        </span>
        <span className="editor-statusbar-item">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          约 {readingTime} 分钟阅读
        </span>
      </div>

      {/* Link dialog */}
      {showLinkDialog && <LinkDialog editor={editor} onClose={() => setShowLinkDialog(false)} />}
    </div>
  );
}