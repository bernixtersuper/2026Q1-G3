import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './auth/LoginPage';
import { SignUpPage } from './auth/SignUpPage';
import { ConfirmSignUpPage } from './auth/ConfirmSignUpPage';
import { ForgotPasswordPage } from './auth/ForgotPasswordPage';
import { RegisterPage } from './auth/RegisterPage';
import { MenuPage } from './menu/MenuPage';
import { AnalyticsPage } from './analytics/AnalyticsPage';
import { TablesPage } from './tables/TablesPage';
import { OrdersPage } from './orders/OrdersPage';
import { ThemePage } from './theme/ThemePage';
import { AppShell } from './shared/components/AppShell';
import { ProtectedRoute } from './shared/components/ProtectedRoute';
import { Toaster } from './components/ui/toaster';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/confirm" element={<ConfirmSignUpPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/admin"
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          <Route index element={<AnalyticsPage />} />
          <Route path="analytics" element={<Navigate to="/admin" replace />} />
          <Route path="menu" element={<MenuPage />} />
          <Route path="tables" element={<TablesPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="theme" element={<ThemePage />} />
        </Route>
        <Route path="/" element={<Navigate to="/admin" replace />} />
        <Route path="*" element={<Navigate to="/admin" replace />} />
      </Routes>
      <Toaster />
    </BrowserRouter>
  );
}
