import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginForm from './LoginForm';
import RegisterForm from './RegisterForm';

const AuthContainer = () => {
    const [isLogin, setIsLogin] = useState(true);
    const [loginSuccess, setLoginSuccess] = useState(false);
    const [message, setMessage] = useState('');
    const navigate = useNavigate();

    const handleLogin = (user, token) => {
        localStorage.setItem('token', token);
        localStorage.setItem('userId', user.id.toString());
        localStorage.setItem('user', JSON.stringify(user));
        setMessage('Login successful!');
        setLoginSuccess(true);
    };

    const handleRegister = (message, user) => {
        setMessage(message);
        setIsLogin(true);
        if (user) {
            localStorage.setItem('token', localStorage.getItem('token'));
            localStorage.setItem('userId', user.id.toString());
            localStorage.setItem('user', JSON.stringify(user));
            setLoginSuccess(true);
        }
    };

    const handleError = (errorMessage) => {
        setMessage(errorMessage);
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setLoginSuccess(false);
        setMessage('Logged out successfully');
        navigate('/login');
    };

    // Navigate after successful login
    useEffect(() => {
        if (loginSuccess) {
            const storedUser = localStorage.getItem('user');
            if (storedUser) {
                const user = JSON.parse(storedUser);
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
                    <h2>Welcome back!</h2>
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

                    {message && <div className={`message ${message.includes('successful') || message.includes('Welcome') ? 'success' : 'error'}`}>{message}</div>}
                </>
            )}
        </div>
    );
};

export default AuthContainer;
