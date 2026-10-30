import { useState, useEffect } from 'react';
import axios from 'axios';

const WalletStatusToggle = () => {
    const [currentStatus, setCurrentStatus] = useState('');
    const [newStatus, setNewStatus] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');

    const fetchStatus = async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/wallet/balance', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setCurrentStatus(response.data.status);
        } catch (error) {
            setMessage('Failed to fetch wallet status');
        }
    };

    useEffect(() => {
        fetchStatus();
    }, []);

    const handleStatusChange = async () => {
        setLoading(true);
        setMessage('');

        try {
            const token = localStorage.getItem('token');
            await axios.patch('/api/wallet/status', { status: newStatus }, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setMessage(`Wallet ${newStatus} successfully`);
            setCurrentStatus(newStatus);
            setNewStatus('');
        } catch (error) {
            setMessage(`Failed to update status: ${error.response?.data?.message || 'Unknown error'}`);
        } finally {
            setLoading(false);
        }
    };

    const getStatusOptions = () => {
        if (currentStatus === 'ACTIVE') {
            return ['INACTIVE'];
        } else if (currentStatus === 'INACTIVE') {
            return ['ACTIVE'];
        } else if (currentStatus === 'FROZEN') {
            return ['FROZEN'];
        }
        return [];
    };

    return (
        <div className="wallet-status-toggle">
            <h3>Wallet Status</h3>
            <div className="status-info">
                <span>Current Status: </span>
                <span className={`status-${currentStatus?.toLowerCase()}`}>{currentStatus || '-'}</span>
            </div>
            
            {currentStatus === 'ACTIVE' || currentStatus === 'INACTIVE' ? (
                <div className="status-actions">
                    <select
                        value={newStatus}
                        onChange={(e) => setNewStatus(e.target.value)}
                    >
                        <option value="">Select new status</option>
                        {getStatusOptions().map(status => (
                            <option key={status} value={status}>{status}</option>
                        ))}
                    </select>
                    <button onClick={handleStatusChange} disabled={loading}>
                        {loading ? 'Updating...' : 'Update Status'}
                    </button>
                </div>
            ) : (
                <div className="status-info">Frozen wallets can only be unfrozen by an administrator</div>
            )}

            {message && <div className={`message ${message.includes('successfully') ? 'success' : 'error'}`}>{message}</div>}
            <button onClick={fetchStatus} style={{ marginTop: '10px' }}>Refresh Status</button>
        </div>
    );
};

export default WalletStatusToggle;
