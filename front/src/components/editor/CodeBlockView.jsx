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
];

function getLanguageLabel(value) {
  return LANGUAGES.find(l => l.value === value)?.label || '纯文本';
}

export default function codeBlockNodeView({ node, getPos, view, extension }) {
  const wrapper = document.createElement('div');
  wrapper.className = 'code-block-wrapper';

  // Language selector
  const langBar = document.createElement('div');
  langBar.className = 'code-block-lang';

  const langBtn = document.createElement('button');
  langBtn.type = 'button';
  langBtn.className = 'code-block-lang-btn';
  langBtn.textContent = getLanguageLabel(node.attrs.language);

  const dropdown = document.createElement('div');
  dropdown.className = 'code-block-lang-dropdown';
  dropdown.style.display = 'none';

  const optionEls = LANGUAGES.map(lang => {
    const option = document.createElement('div');
    option.className = `code-block-lang-option${lang.value === node.attrs.language ? ' active' : ''}`;
    option.textContent = lang.label;
    option.addEventListener('mousedown', (e) => {
      e.preventDefault();
      const pos = typeof getPos === 'function' ? getPos() : getPos;
      view.dispatch(
        view.state.tr.setNodeMarkup(pos, undefined, {
          language: lang.value,
        })
      );
      dropdown.style.display = 'none';
    });
    dropdown.appendChild(option);
    return option;
  });

  langBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    dropdown.style.display = dropdown.style.display === 'none' ? 'block' : 'none';
  });

  langBar.appendChild(langBtn);
  langBar.appendChild(dropdown);
  wrapper.appendChild(langBar);

  // Code content
  const pre = document.createElement('pre');
  const code = document.createElement('code');
  if (node.attrs.language) {
    code.setAttribute('data-language', node.attrs.language);
    code.className = `language-${node.attrs.language}`;
  }
  pre.appendChild(code);
  wrapper.appendChild(pre);

  // Close dropdown on outside click
  const handleOutsideClick = (e) => {
    if (!langBar.contains(e.target)) {
      dropdown.style.display = 'none';
    }
  };
  document.addEventListener('mousedown', handleOutsideClick);

  return {
    dom: wrapper,
    contentDOM: code,
    update: (updatedNode) => {
      if (updatedNode.type.name !== 'codeBlock') return false;
      const lang = updatedNode.attrs.language || '';
      langBtn.textContent = getLanguageLabel(lang);
      code.className = lang ? `language-${lang}` : '';
      if (lang) {
        code.setAttribute('data-language', lang);
      } else {
        code.removeAttribute('data-language');
      }
      optionEls.forEach((opt, i) => {
        opt.className = `code-block-lang-option${LANGUAGES[i].value === lang ? ' active' : ''}`;
      });
      return true;
    },
    destroy: () => {
      document.removeEventListener('mousedown', handleOutsideClick);
    },
  };
}
