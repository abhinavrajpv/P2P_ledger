import { useState } from 'react';
import axios from 'axios';

const DepositForm = () => {
    const [amount, setAmount] = useState('');
    const [reference, setReference] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setSuccess('');

        try {
            const token = localStorage.getItem('token');
            await axios.post('/api/wallet/deposit', {
                amount: parseFloat(amount),
                reference
            }, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setSuccess('Deposit processed successfully');
            setAmount('');
            setReference('');
        } catch (error) {
            setSuccess(`Deposit failed: ${error.response?.data?.message || 'Unknown error'}`);
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
            {success && <div className={`message ${success.includes('successfully') ? 'success' : 'error'}`}>{success}</div>}
        </div>
    );
};

export default DepositForm;
