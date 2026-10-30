import { useState, useEffect } from 'react';
import axios from 'axios';

const MaskingToggle = () => {
    const [enabled, setEnabled] = useState(false);
    const [pin, setPin] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [messageType, setMessageType] = useState('');

    const fetchStatus = async () => {
        setLoading(true);
        setMessage('');
        try {
            const response = await axios.get('/api/masking/status');
            setEnabled(response.data.enabled);
            setPin('');
            setMessage('Masking status refreshed successfully');
            setMessageType('success');
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch masking status';
            setMessage(`Failed to fetch masking status: ${errorMessage}`);
            setMessageType('error');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStatus();
    }, []);

    const handleToggle = async (enable) => {
        if (!pin || pin.length !== 4) {
            setMessage('Please enter your 4-digit PIN');
            setMessageType('error');
            return;
        }

        setLoading(true);
        setMessage('');
        setMessageType('');

        try {
            const endpoint = enable ? '/api/masking/enable' : '/api/masking/disable';
            await axios.post(endpoint, { pin });
            setEnabled(enable);
            setMessage(`Masking ${enable ? 'enabled' : 'disabled'} successfully`);
            setMessageType('success');
            setPin('');
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || `Failed to ${enable ? 'enable' : 'disable'} masking`;
            setMessage(`Failed to ${enable ? 'enable' : 'disable'} masking: ${errorMessage}`);
            setMessageType('error');
        } finally {
            setLoading(false);
        }
    };

    const toggleEnabled = () => {
        handleToggle(!enabled);
    };

    return (
        <div className="masking-toggle">
            <h3>Transaction Privacy</h3>
            <div className="masking-status">
                <span>Current Status: </span>
                <span className={`status-${enabled ? 'enabled' : 'disabled'}`}>
                    {enabled ? 'Privacy Enabled' : 'Privacy Disabled'}
                </span>
            </div>
            <div className="masking-info">
                <p>
                    When enabled, your transaction history will hide counterparty details
                    (IDs, names, emails, phone numbers). Amount, date, and status remain visible.
                </p>
            </div>
            <div className="masking-actions">
                <button onClick={toggleEnabled} disabled={loading}>
                    {loading ? 'Processing...' : enabled ? 'Disable Privacy' : 'Enable Privacy'}
                </button>
            </div>
            <div className="pin-input">
                <input
                    type="password"
                    value={pin}
                    onChange={(e) => {
                        const value = e.target.value.replace(/\D/g, '').slice(0, 4);
                        setPin(value);
                    }}
                    placeholder="Enter your 4-digit PIN"
                    maxLength={4}
                />
                <span className="pin-hint">Enter PIN to {enabled ? 'disable' : 'enable'} privacy</span>
            </div>
            {message && <div className={`message ${messageType}`}>{message}</div>}
            <button onClick={fetchStatus} disabled={loading} className="refresh-status">
                {loading ? 'Loading...' : 'Refresh Status'}
            </button>
        </div>
    );
};

export default MaskingToggle;
