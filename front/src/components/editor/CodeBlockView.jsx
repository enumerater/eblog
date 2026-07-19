const LANGUAGES = [
  { value: '', label: '纯文本' },
  { value: 'javascript', label: 'JavaScript' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'python', label: 'Python' },
  { value: 'java', label: 'Java' },
  { value: 'go', label: 'Go' },
  { value: 'rust', label: 'Rust' },
  { value: 'c', label: 'C' },
  { value: 'cpp', label: 'C++' },
  { value: 'csharp', label: 'C#' },
  { value: 'php', label: 'PHP' },
  { value: 'ruby', label: 'Ruby' },
  { value: 'swift', label: 'Swift' },
  { value: 'kotlin', label: 'Kotlin' },
  { value: 'sql', label: 'SQL' },
  { value: 'bash', label: 'Bash' },
  { value: 'shell', label: 'Shell' },
  { value: 'json', label: 'JSON' },
  { value: 'xml', label: 'XML' },
  { value: 'yaml', label: 'YAML' },
  { value: 'css', label: 'CSS' },
  { value: 'scss', label: 'SCSS' },
  { value: 'html', label: 'HTML' },
  { value: 'markdown', label: 'Markdown' },
  { value: 'nginx', label: 'Nginx' },
  { value: 'lua', label: 'Lua' },
  { value: 'perl', label: 'Perl' },
  { value: 'dart', label: 'Dart' },
  { value: 'dockerfile', label: 'Dockerfile' },
  { value: 'diff', label: 'Diff' },
  { value: 'graphql', label: 'GraphQL' },
  { value: 'makefile', label: 'Makefile' },
  { value: 'powershell', label: 'PowerShell' },
  { value: 'r', label: 'R' },
  { value: 'sass', label: 'Sass' },
  { value: 'scala', label: 'Scala' },
  { value: 'toml', label: 'TOML' },
];

function getLanguageLabel(value) {
  return LANGUAGES.find(l => l.value === value)?.label || '纯文本';
}

function countLines(text) {
  // ProseMirror code blocks always end with \n, which creates a visible
  // blank line in contenteditable. Count ALL lines including that trailing one.
  if (!text || text === '\n') return 1;
  const lines = text.split('\n');
  return lines.length;
}

function renderLineNumbers(count) {
  const frag = document.createDocumentFragment();
  for (let i = 1; i <= count; i++) {
    const span = document.createElement('span');
    span.className = 'line-number';
    span.textContent = i;
    frag.appendChild(span);
  }
  return frag;
}

export default function codeBlockNodeView({ node, getPos, view, extension }) {
  const initialLang = node.attrs.language || '';

  const wrapper = document.createElement('div');
  wrapper.className = 'code-block-wrapper';

  // --- Line numbers gutter ---
  const lineNumbers = document.createElement('div');
  lineNumbers.className = 'code-block-line-numbers';
  wrapper.appendChild(lineNumbers);

  // --- ContentDOM: this is the editable pre element ---
  // ProseMirror will put the content (with syntax highlighting decorations) here
  const pre = document.createElement('pre');
  pre.className = 'code-block-content';
  if (initialLang) {
    pre.setAttribute('data-language', initialLang);
  }
  wrapper.appendChild(pre);

  // --- Toolbar ---
  const toolBar = document.createElement('div');
  toolBar.className = 'code-block-toolbar';

  // Language dropdown
  const langBtn = document.createElement('button');
  langBtn.type = 'button';
  langBtn.className = 'code-block-toolbar-btn';
  langBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg> ${getLanguageLabel(initialLang)}`;

  const dropdown = document.createElement('div');
  dropdown.className = 'code-block-lang-dropdown';
  dropdown.style.display = 'none';

  const optionEls = LANGUAGES.map(lang => {
    const option = document.createElement('div');
    option.className = 'code-block-lang-option' + (lang.value === initialLang ? ' active' : '');
    option.textContent = lang.label;
    option.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      const pos = typeof getPos === 'function' ? getPos() : getPos;
      if (pos === undefined || pos === null) return;
      view.dispatch(
        view.state.tr.setNodeMarkup(pos, undefined, { language: lang.value })
      );
      dropdown.style.display = 'none';
    });
    dropdown.appendChild(option);
    return option;
  });

  langBtn.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    dropdown.style.display = dropdown.style.display === 'none' ? 'block' : 'none';
  });

  toolBar.appendChild(langBtn);
  toolBar.appendChild(dropdown);

  // Copy button
  const copyBtn = document.createElement('button');
  copyBtn.type = 'button';
  copyBtn.className = 'code-block-toolbar-btn code-block-copy-btn';
  copyBtn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>';
  copyBtn.title = '复制代码';

  copyBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    navigator.clipboard.writeText(pre.textContent).then(() => {
      const original = copyBtn.innerHTML;
      copyBtn.innerHTML = '<span style="color:#98c379">已复制</span>';
      copyBtn.classList.add('copied');
      setTimeout(() => {
        copyBtn.innerHTML = original;
        copyBtn.classList.remove('copied');
      }, 2000);
    });
  });

  toolBar.appendChild(copyBtn);
  wrapper.appendChild(toolBar);

  // --- Initial line numbers ---
  const initialCount = countLines(node.textContent);
  lineNumbers.appendChild(renderLineNumbers(initialCount));

  // --- MutationObserver: update line numbers when content changes ---
  const observer = new MutationObserver(() => {
    const count = countLines(pre.textContent);
    lineNumbers.innerHTML = '';
    lineNumbers.appendChild(renderLineNumbers(count));
  });
  observer.observe(pre, { childList: true, characterData: true, subtree: true });

  // Close dropdown on outside click
  const handleOutsideClick = (e) => {
    if (!toolBar.contains(e.target)) {
      dropdown.style.display = 'none';
    }
  };
  document.addEventListener('mousedown', handleOutsideClick);

  return {
    dom: wrapper,
    contentDOM: pre,
    update: (updatedNode) => {
      if (updatedNode.type.name !== 'codeBlock') return false;
      const lang = updatedNode.attrs.language || '';
      langBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg> ${getLanguageLabel(lang)}`;
      pre.setAttribute('data-language', lang);
      optionEls.forEach((opt, i) => {
        opt.className = 'code-block-lang-option' + (LANGUAGES[i].value === lang ? ' active' : '');
      });
      return true;
    },
    destroy: () => {
      observer.disconnect();
      document.removeEventListener('mousedown', handleOutsideClick);
    },
    ignoreMutation: (mutation) => {
      // Only ignore mutations to elements outside the contentDOM (pre)
      // This ensures user edits to the code block are processed by ProseMirror
      return !pre.contains(mutation.target);
    },
  };
}