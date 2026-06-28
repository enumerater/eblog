import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { auth } from './api';
import Navbar from './components/Navbar';
import ScrollToTop from './components/effects/ScrollToTop';
import Home from './pages/Home';
import ArticleDetail from './pages/ArticleDetail';
import ArticleEditor from './pages/ArticleEditor';
import Admin from './pages/Admin';
import Login from './pages/Login';

function ProtectedRoute({ children }) {
  if (!auth.isAuthed()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <ScrollToTop />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/article/:id" element={<ArticleDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/editor" element={
            <ProtectedRoute><ArticleEditor /></ProtectedRoute>
          } />
          <Route path="/admin" element={
            <ProtectedRoute><Admin /></ProtectedRoute>
          } />
        </Routes>
      </main>
    </BrowserRouter>
  );
}
