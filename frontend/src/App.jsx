import { Route, Routes, useLocation } from 'react-router-dom';
import Header from './components/Header.jsx';
import HomePage from './pages/HomePage.jsx';
import AuthPage from './pages/AuthPage.jsx';
import OwnerPage from './pages/OwnerPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import ResidentPage from './pages/ResidentPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import PropertyDetailPage from './pages/PropertyDetailPage.jsx';
import EmailActionPage from './pages/EmailActionPage.jsx';
import StaffPage from './pages/StaffPage.jsx';

export default function App() {
  const location = useLocation();
  const workspace = /^\/(owner|resident|admin|staff)(\/|$)/.test(location.pathname);
  return (
    <>
      {!workspace && <Header />}
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<AuthPage />} />
        <Route path="/register" element={<AuthPage />} />
        <Route path="/verify-email" element={<EmailActionPage />} />
        <Route path="/password-reset" element={<EmailActionPage />} />
        <Route path="/properties/:propertyId" element={<PropertyDetailPage />} />
        <Route path="/owner/*" element={<OwnerPage />} />
        <Route path="/resident/*" element={<ResidentPage />} />
        <Route path="/admin/*" element={<AdminPage />} />
        <Route path="/staff/*" element={<StaffPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </>
  );
}
