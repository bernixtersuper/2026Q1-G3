import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { PublicMenuPage } from './menu/PublicMenuPage';
import { TableMenu } from './pages/TableMenu';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/table/:qrToken" element={<TableMenu />} />
        <Route path="/menu/:slug" element={<PublicMenuPage />} />
        <Route path="*" element={<Navigate to="/menu/demo" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
