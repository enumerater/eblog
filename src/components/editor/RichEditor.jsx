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
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight';
import { common, createLowlight } from 'lowlight';
import { marked } from 'marked';
import { api } from '../../api';
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
});

function looksLikeMarkdown(text) {
  const lines = text.split('\n');
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

// --- SVG Icon set for toolbar ---
const I = {
  bold: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/><path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"/></svg>,
  italic: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="19" y1="4" x2="10" y2="4"/><line x1="14" y1="20" x2="5" y2="20"/><line x1="15" y1="4" x2="9" y2="20"/></svg>,
  underline: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M6 3v7a6 6 0 0 0 6 6 6 6 0 0 0 6-6V3"/><line x1="4" y1="21" x2="20" y2="21"/></svg>,
  strikethrough: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M17.3 4.9c-2.3-.6-4.4-1-6.2-.9-2.7 0-5.3.7-5.3 3.6 0 1.5 1.8 3.3 7.2 3.3"/><path d="M13 15.1c1.3.3 2.5.9 2.5 2.5 0 2.5-3.5 3.5-6 3.5-1.5 0-3.1-.3-4.5-.7"/><line x1="2" y1="12" x2="22" y2="12"/></svg>,
  highlight: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="m9 11-6 6v3h9l3-3"/><path d="m22 12-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4"/></svg>,
  h1: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M17 12l3-2v8"/></svg>,
  h2: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M21 18h-4c0-4 4-3 4-6 0-1.5-2-2.5-4-1"/></svg>,
  h3: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 12h8"/><path d="M4 18V6"/><path d="M12 18V6"/><path d="M17.5 10.5c1.7-1 3.5 0 3.5 1.5a2 2 0 0 1-2 2"/><path d="M17 17.5c2 1.5 4 .3 4-1.5a2 2 0 0 0-2-2"/></svg>,
  alignLeft: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="17" y1="10" x2="3" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/><line x1="21" y1="14" x2="3" y2="14"/><line x1="17" y1="18" x2="3" y2="18"/></svg>,
  alignCenter: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="10" x2="6" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/><line x1="21" y1="14" x2="3" y2="14"/><line x1="18" y1="18" x2="6" y2="18"/></svg>,
  alignRight: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="21" y1="10" x2="7" y2="10"/><line x1="21" y1="6" x2="3" y2="6"/><line x1="21" y1="14" x2="3" y2="14"/><line x1="21" y1="18" x2="7" y2="18"/></svg>,
  bulletList: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><circle cx="3.5" cy="6" r="1.5" fill="currentColor" stroke="none"/><circle cx="3.5" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="3.5" cy="18" r="1.5" fill="currentColor" stroke="none"/></svg>,
  orderedList: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="10" y1="6" x2="21" y2="6"/><line x1="10" y1="12" x2="21" y2="12"/><line x1="10" y1="18" x2="21" y2="18"/><text x="2" y="8" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">1</text><text x="2" y="14" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">2</text><text x="2" y="20" fontSize="7" fill="currentColor" stroke="none" fontFamily="sans-serif">3</text></svg>,
  blockquote: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3z"/></svg>,
  codeBlock: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>,
  hr: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="2" y1="12" x2="22" y2="12"/></svg>,
  table: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/><line x1="15" y1="3" x2="15" y2="21"/></svg>,
  image: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>,
  import: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>,
  undo: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>,
  redo: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>,
};

export default function RichEditor({ content, onChange }) {
  const fileInputRef = useRef(null);
  const imageInputRef = useRef(null);
  const [uploadingImages, setUploadingImages] = useState(false);
  const editorRef = useRef(null);

  const uploadAndInsertImage = useCallback(async (file) => {
    if (!editorRef.current) return;
    setUploadingImages(true);
    try {
      const result = await api.uploadImage(file);
      const url = result.url;
      editorRef.current.chain().focus().setImage({ src: url }).run();
    } catch (err) {
      alert('图片上传失败: ' + (err.message || '未知错误'));
    } finally {
      setUploadingImages(false);
    }
  }, []);

  const handleImageFileChange = useCallback((e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    uploadAndInsertImage(file);
    e.target.value = '';
  }, [uploadAndInsertImage]);

  // Sync editorRef when editor changes
  useEffect(() => {
    editorRef.current = editor;
  }, [editor]);

  const editor = useEditor({
    extensions: [
      StarterKit.configure({ codeBlock: false }),
      CustomCodeBlock,
      Underline,
      Highlight,
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
      SlashCommands,
    ],
    content: content || '',
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML());
    },
  });

  // Capture-phase paste: intercept before ProseMirror
  useEffect(() => {
    if (!editor) return;

    const handlePaste = (event) => {
      // Check for image files first (screenshots, copied images)
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

      // Check for image files first
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

  if (!editor) return null;

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

  return (
    <div className="rich-editor">
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
          <ToolBtn onClick={() => editor.chain().focus().toggleHighlight().run()} active={editor.isActive('highlight')} title="高亮">
            {I.highlight}
          </ToolBtn>
        </div>

        <div className="toolbar-divider" />

        <div className="toolbar-group">
          <ToolBtn onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()} active={editor.isActive('heading', { level: 1 })} title="标题 1">
            {I.h1}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()} active={editor.isActive('heading', { level: 2 })} title="标题 2">
            {I.h2}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()} active={editor.isActive('heading', { level: 3 })} title="标题 3">
            {I.h3}
          </ToolBtn>
        </div>

        <div className="toolbar-divider" />

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

        <div className="toolbar-group">
          <ToolBtn onClick={() => editor.chain().focus().toggleBulletList().run()} active={editor.isActive('bulletList')} title="无序列表">
            {I.bulletList}
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().toggleOrderedList().run()} active={editor.isActive('orderedList')} title="有序列表">
            {I.orderedList}
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

        <div className="toolbar-group">
          <ToolBtn onClick={() => editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()} active={editor.isActive('table')} title="插入表格">
            {I.table}
          </ToolBtn>
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
        </div>

        <div className="toolbar-divider" />

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
        </div>
      </div>

      <EditorBubbleMenu editor={editor} />
      <EditorContent editor={editor} className="editor-content" />
    </div>
  );
}
