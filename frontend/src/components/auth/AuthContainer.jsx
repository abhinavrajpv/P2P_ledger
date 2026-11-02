import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginForm from './LoginForm';
import RegisterForm from './RegisterForm';
import { setAuth, clearAuth, getUser } from '../../auth';
import './AuthContainer.css';

const AuthContainer = () => {
    const [isLogin, setIsLogin] = useState(true);
    const [loginSuccess, setLoginSuccess] = useState(false);
    const [message, setMessage] = useState('');
    const navigate = useNavigate();

    const handleLogin = (user, token) => {
        setAuth(token, user);
        setMessage('Login successful!');
        setLoginSuccess(true);
    };

    const handleRegister = (msg, user) => {
        setMessage(msg);
        setIsLogin(true);
        if (user) {
            setLoginSuccess(true);
        }
    };

    const handleError = (errorMessage) => {
        setMessage(errorMessage);
    };

    const handleLogout = () => {
        clearAuth();
        setLoginSuccess(false);
        setMessage('Logged out successfully');
        navigate('/login');
    };

    // Navigate after successful login — reads role from sessionStorage
    useEffect(() => {
        if (loginSuccess) {
            const user = getUser();
            if (user) {
                if (user.role === 'ADMIN') {
                    navigate('/admin');
                } else {
                    navigate('/wallet');
                }
            }
        }
    }, [loginSuccess, navigate]);

    return (
        <div className="auth-page">
            <div className="auth-background">
                <div className="auth-overlay"></div>
            </div>
            
            <div className="auth-content">
                <div className="auth-card">
                    <div className="auth-header">
                        <h1 className="auth-logo">💳 P2P Ledger</h1>
                        <p className="auth-subtitle">Secure peer-to-peer payments</p>
                    </div>

                    <div className="auth-toggle">
                        <button
                            className={isLogin ? 'toggle-btn active' : 'toggle-btn'}
                            onClick={() => { setIsLogin(true); setMessage(''); }}
                        >
                            Login
                        </button>
                        <button
                            className={!isLogin ? 'toggle-btn active' : 'toggle-btn'}
                            onClick={() => { setIsLogin(false); setMessage(''); }}
                        >
                            Register
                        </button>
                        <div className={`toggle-slider ${!isLogin ? 'right' : ''}`}></div>
                    </div>

                    {loginSuccess ? (
                        <div className="success-message">
                            <div className="success-icon">✓</div>
                            <p>{message}</p>
                            <button className="btn-logout" onClick={handleLogout}>Logout</button>
                        </div>
                    ) : (
                        <>
                            <div className="form-container">
                                {isLogin ? (
                                    <LoginForm onLogin={handleLogin} onError={handleError} />
                                ) : (
                                    <RegisterForm onRegister={handleRegister} onError={handleError} />
                                )}
                            </div>
                            {message && (
                                <div className={`message-alert ${message.toLowerCase().includes('success') || message.toLowerCase().includes('welcome') ? 'success' : 'error'}`}>
                                    {message}
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AuthContainer;
