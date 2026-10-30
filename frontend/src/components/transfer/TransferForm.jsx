import { useState, useEffect } from 'react';
import axios from 'axios';

const TransferForm = ({ onTransferSuccess, prefilledReceiverId, onReceiverUsed }) => {
    const [receiverId, setReceiverId] = useState('');
    const [amount, setAmount] = useState('');
    const [pin, setPin] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showSuccess, setShowSuccess] = useState(false);
    const [successData, setSuccessData] = useState(null);

    // Auto-fill receiver ID when selected from UserSearch
    useEffect(() => {
        if (prefilledReceiverId != null) {
            setReceiverId(String(prefilledReceiverId));
            onReceiverUsed?.();
        }
    }, [prefilledReceiverId]);

    const generateRequestId = () => {
        return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setShowSuccess(false);

        // Client-side validation
        if (!pin || pin.length !== 4 || !/^\d{4}$/.test(pin)) {
            setError('Transaction PIN must be exactly 4 digits');
            setLoading(false);
            return;
        }

        try {
            const requestId = generateRequestId();
            const response = await axios.post('/api/transfers', {
                receiverId: Number(receiverId),
                amount: parseFloat(amount),
                requestId,
                pin
            });
            setSuccessData(response.data);
            setShowSuccess(true);
            setReceiverId('');
            setAmount('');
            setPin('');
            onTransferSuccess?.(response.data);
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'Transfer failed');
        } finally {
            setLoading(false);
        }
    };

    if (showSuccess) {
        return (
            <div className="transfer-form">
                <div className="message success">
                    <h3>Transfer Initiated!</h3>
                    <p>Transfer of ₹{parseFloat(successData?.amount || 0).toFixed(2)} has been initiated successfully.</p>
                    <p>It will be settled in 3 business days.</p>
                    <button onClick={() => { setShowSuccess(false); setSuccessData(null); }}>Make Another Transfer</button>
                </div>
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
                        min="1"
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="amount">Amount (₹)</label>
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
                        onChange={(e) => {
                            const val = e.target.value.replace(/\D/g, '').slice(0, 4);
                            setPin(val);
                        }}
                        placeholder="Enter your 4-digit PIN"
                        maxLength={4}
                        required
                    />
                </div>
                {error && <div className="message error">{error}</div>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Processing...' : 'Send Transfer'}
                </button>
            </form>
        </div>
    );
};

export default TransferForm;
