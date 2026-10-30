import { useState } from 'react';
import axios from 'axios';
import { setAuth } from '../../auth';

const RegisterForm = ({ onRegister, onError }) => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [transactionPin, setTransactionPin] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const response = await axios.post('/api/auth/register', {
                name,
                email,
                phone,
                password,
                transactionPin
            });

            // After successful registration, log the user in
            const loginResponse = await axios.post('/api/auth/login', {
                identifier: email,
                password
            });

            // Store token and userId in sessionStorage (per-tab, not shared)
            setAuth(loginResponse.data.token, loginResponse.data.user);

            onRegister(response.data.message, loginResponse.data.user);
        } catch (error) {
            onError(error.response?.data?.message || 'Registration failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-form">
            <h2>Register</h2>
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="name">Name</label>
                    <input
                        type="text"
                        id="name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="email">Email</label>
                    <input
                        type="email"
                        id="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="phone">Phone</label>
                    <input
                        type="tel"
                        id="phone"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="password">Password (min 8 chars, 1 uppercase, 1 lowercase, 1 digit)</label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="transactionPin">Transaction PIN (4 digits)</label>
                    <input
                        type="password"
                        id="transactionPin"
                        value={transactionPin}
                        onChange={(e) => setTransactionPin(e.target.value)}
                        required
                        maxLength={4}
                    />
                </div>
                <button type="submit" disabled={loading}>
                    {loading ? 'Registering...' : 'Register'}
                </button>
            </form>
        </div>
    );
};

export default RegisterForm;
