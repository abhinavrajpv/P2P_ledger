import { useState } from 'react';
import axios from 'axios';

const TransferForm = ({ onTransferSuccess, onError }) => {
    const [receiverId, setReceiverId] = useState('');
    const [amount, setAmount] = useState('');
    const [pin, setPin] = useState('');
    const [loading, setLoading] = useState(false);
    const [showSuccess, setShowSuccess] = useState(false);

    const generateRequestId = () => {
        return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setShowSuccess(false);

        try {
            const token = localStorage.getItem('token');
            const requestId = generateRequestId();
            const response = await axios.post('/api/transfers', {
                receiverId: parseInt(receiverId),
                amount: parseFloat(amount),
                requestId,
                pin
            }, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setShowSuccess(true);
            setReceiverId('');
            setAmount('');
            setPin('');
            onTransferSuccess?.(response.data);
        } catch (error) {
            onError?.(error.response?.data?.message || 'Transfer failed');
        } finally {
            setLoading(false);
        }
    };

    if (showSuccess) {
        return (
            <div className="transfer-success">
                <h3>Transfer Successful!</h3>
                <p>Your transfer has been initiated and will be settled in 3 business days.</p>
                <button onClick={() => setShowSuccess(false)}>Make Another Transfer</button>
            </div>
        );
    }

    return (
        <div className="transfer-form">
            <h3>Send Money</h3>
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="receiverId">Receiver ID</label>
                    <input
                        type="number"
                        id="receiverId"
                        value={receiverId}
                        onChange={(e) => setReceiverId(e.target.value)}
                        placeholder="Enter receiver's user ID"
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="amount">Amount</label>
                    <input
                        type="number"
                        id="amount"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder="Enter amount"
                        step="0.01"
                        min="0.01"
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="pin">Transaction PIN</label>
                    <input
                        type="password"
                        id="pin"
                        value={pin}
                        onChange={(e) => setPin(e.target.value)}
                        placeholder="Enter your 4-digit PIN"
                        maxLength={4}
                        required
                    />
                </div>
                <button type="submit" disabled={loading}>
                    {loading ? 'Processing...' : 'Send Transfer'}
                </button>
            </form>
        </div>
    );
};

export default TransferForm;
