import { useState, useEffect } from 'react';
import axios from 'axios';

const ReconciliationReport = () => {
    const [todayRecon, setTodayRecon] = useState(null);
    const [loading, setLoading] = useState(false);

    const fetchTodayReconciliation = async () => {
        setLoading(true);
        try {
            const response = await axios.get('/api/admin/reconciliation/today');
            setTodayRecon(response.data);
        } catch (error) {
            console.error('Failed to fetch today\'s reconciliation:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTodayReconciliation();
        // Auto-refresh today's reconciliation every 60 seconds
        const interval = setInterval(fetchTodayReconciliation, 60000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="reconciliation-report">
            <h3>Daily Reconciliation Report</h3>
            
            {loading && <div className="loading">Loading today's reconciliation...</div>}
            
            {/* Today's Reconciliation Status */}
            {!loading && todayRecon && (
                <div className={`today-reconciliation ${todayRecon.balanced ? 'balanced' : 'unbalanced'}`}>
                    <h4>Today's Reconciliation Status</h4>
                    <div className="recon-status-grid">
                        <div className="recon-card">
                            <span className="recon-label">Total Debits</span>
                            <span className="recon-value">₹{parseFloat(todayRecon.totalDebits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                            <small>{todayRecon.debitCount} entries</small>
                        </div>
                        <div className="recon-card">
                            <span className="recon-label">Total Credits</span>
                            <span className="recon-value">₹{parseFloat(todayRecon.totalCredits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                            <small>{todayRecon.creditCount} entries</small>
                        </div>
                        <div className="recon-card pending-card">
                            <span className="recon-label">⏳ Pending Settlement</span>
                            <span className="recon-value pending">₹{parseFloat(todayRecon.pendingSettlement || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                            <small>{todayRecon.pendingCount || 0} transfers</small>
                        </div>
                        <div className={`recon-card status ${todayRecon.balanced ? 'success' : 'error'}`}>
                            <span className="recon-label">STATUS</span>
                            <span className="recon-status-value">
                                {todayRecon.balanced ? '✓ BALANCED' : '⚠️ MISMATCH DETECTED'}
                            </span>
                            {!todayRecon.balanced && (
                                <div className="mismatch-details">
                                    <strong>Mismatch Amount:</strong>
                                    <span className="mismatch-value">₹{parseFloat(todayRecon.mismatch || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                                </div>
                            )}
                        </div>
                    </div>
                    <button onClick={fetchTodayReconciliation} className="refresh-btn">
                        Refresh Today's Status
                    </button>
                </div>
            )}
        </div>
    );
};

export default ReconciliationReport;
