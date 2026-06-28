import './Footer.css';

export default function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="site-footer">
      <div className="footer-inner">
        <div className="footer-left">
          <span className="footer-brand">enumerate</span>
          <span className="footer-sep">&middot;</span>
          <span className="footer-copy">&copy; {year}</span>
        </div>
        <div className="footer-right">
          <a href="https://github.com/enumerater" target="_blank" rel="noopener noreferrer">GitHub</a>
        </div>
      </div>
    </footer>
  );
}
