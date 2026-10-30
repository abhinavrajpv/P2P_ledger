import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginForm from './LoginForm';
import RegisterForm from './RegisterForm';
import { setAuth, clearAuth, getUser } from '../../auth';

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
        <div className="auth-container">
            <div className="auth-toggle">
                <button
                    className={isLogin ? 'active' : ''}
                    onClick={() => { setIsLogin(true); setMessage(''); }}
                >
                    Login
                </button>
                <button
                    className={!isLogin ? 'active' : ''}
                    onClick={() => { setIsLogin(false); setMessage(''); }}
                >
                    Register
                </button>
            </div>

            {loginSuccess ? (
                <div className="success-message">
                    <p>{message}</p>
                    <button onClick={handleLogout}>Logout</button>
                </div>
            ) : (
                <>
                    {isLogin ? (
                        <LoginForm onLogin={handleLogin} onError={handleError} />
                    ) : (
                        <RegisterForm onRegister={handleRegister} onError={handleError} />
                    )}
                    {message && (
                        <div className={`message ${message.toLowerCase().includes('success') || message.toLowerCase().includes('welcome') ? 'success' : 'error'}`}>
                            {message}
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default AuthContainer;
