import { Link } from 'react-router-dom';
import './ArticleCard.css';

export default function ArticleCard({ article }) {
  const date = new Date(article.createdAt).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric',
  });

  return (
    <div className="article-card">
      <div className="article-card-date">{date}</div>
      <h2 className="article-card-title">
        <Link to={`/article/${article.id}`}>{article.title}</Link>
      </h2>
      <p className="article-card-summary">
        {article.summary || article.content?.replace(/<[^>]*>/g, '').slice(0, 200) || ''}
      </p>
      <div className="article-card-footer">
        {article.tags?.length > 0 && (
          <div className="article-card-tags">
            {article.tags.map(t => <span key={t} className="tag">{t}</span>)}
          </div>
        )}
        <Link to={`/article/${article.id}`} className="article-card-readmore">阅读全文</Link>
      </div>
    </div>
  );
}
