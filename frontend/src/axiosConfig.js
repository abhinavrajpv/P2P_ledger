import axios from 'axios';
import { getToken, clearAuth } from './auth';

/**
 * Global Axios request interceptor.
 * Automatically attaches the per-tab sessionStorage token to every request.
 * Components no longer need to manually read the token.
 */
axios.interceptors.request.use(
    (config) => {
        const token = getToken();
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

/**
 * Global Axios response interceptor.
 * Handles 401/403 errors by clearing auth and redirecting to login.
 */
axios.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            // Token expired or invalid - clear auth and reload to show login
            console.error('Authentication error - clearing session');
            clearAuth();
            window.location.href = '/';
        }
        return Promise.reject(error);
    }
);

export default axios;
