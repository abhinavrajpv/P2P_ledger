import axios from 'axios';
import { getToken } from './auth';

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

export default axios;
