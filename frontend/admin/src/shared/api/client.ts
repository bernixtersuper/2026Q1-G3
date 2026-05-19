import axios from 'axios';

export const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const API_URL = apiBaseUrl;

export const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  // Auth endpoints carry a Cognito Bearer token set by the caller; do not overwrite.
  const requestUrl = String(config.url ?? '');
  if (requestUrl.startsWith('/api/auth/')) {
    return config;
  }

  const token = localStorage.getItem('md_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = String(error.config?.url ?? '');
    if (error.response?.status === 401 && !requestUrl.startsWith('/api/auth/')) {
      localStorage.removeItem('md_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
