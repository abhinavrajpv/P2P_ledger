import { useState } from 'react';
import axios from 'axios';

const DepositForm = ({ onSuccess }) => {
    const [amount, setAmount] = useState('');
    const [reference, setReference] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');
        setIsError(false);

        try {
            const response = await axios.post('/api/wallet/deposit', {
                amount: parseFloat(amount),
                reference
            });
            setMessage(`Deposit of ₹${parseFloat(amount).toFixed(2)} processed successfully! New balance: ₹${parseFloat(response.data.newBalance).toFixed(2)}`);
            setIsError(false);
            setAmount('');
            setReference('');
            onSuccess?.(); // notify parent to refresh balance
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Deposit failed';
            setMessage(`Deposit failed: ${errorMessage}`);
            setIsError(true);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="deposit-form">
            <h3>Deposit Funds</h3>
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="amount">Amount</label>
                    <input
                        type="number"
                        id="amount"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder="Enter amount"
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="reference">Reference</label>
                    <input
                        type="text"
                        id="reference"
                        value={reference}
                        onChange={(e) => setReference(e.target.value)}
                        placeholder="Optional reference"
                    />
                </div>
                <button type="submit" disabled={loading}>
                    {loading ? 'Processing...' : 'Deposit'}
                </button>
            </form>
            {message && <div className={`message ${isError ? 'error' : 'success'}`}>{message}</div>}
        </div>
    );
};

export default DepositForm;
