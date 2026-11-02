import { useState, useEffect } from 'react';
import axios from 'axios';

const ReconciliationReport = () => {
    const [dailySummary, setDailySummary] = useState(null);
    const [loading, setLoading] = useState(false);

    const fetchDailySummary = async () => {
        setLoading(true);
        try {
            // Fetch today's daily summary
            const today = new Date().toISOString().split('T')[0];
            const startDate = today;
            const endDate = today;
            const summaryResponse = await axios.get(`/api/admin/reconciliation?startDate=${startDate}&endDate=${endDate}`);
            if (summaryResponse.data && summaryResponse.data.length > 0) {
                setDailySummary(summaryResponse.data[0]);
            } else {
                setDailySummary(null);
            }
        } catch (error) {
            console.error('Failed to fetch daily summary:', error.response?.data?.message || error.message);
            setDailySummary(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDailySummary();
    }, []);

    return (
        <div className="reconciliation-report">
            <h3>Daily Summary & Reconciliation Report</h3>
            <p className="info-text">Updated every 5 minutes by automated job</p>
            
            {loading && <div className="loading">Loading daily summary...</div>}
            
            {!loading && !dailySummary && (
                <div className="no-data">
                    <p>Daily summary not yet generated. The job runs every 5 minutes.</p>
                </div>
            )}
            
            {/* Daily Summary Table */}
            {!loading && dailySummary && (
                <div className="daily-summary-section">
                    <h4>Daily Summary</h4>
                    <table className="summary-table">
                        <tbody>
                            <tr>
                                <td><strong>Total number of transfers initiated:</strong></td>
                                <td>{dailySummary.totalTransfersInitiated || 0}</td>
                                <td>₹{parseFloat(dailySummary.totalAmountInitiated || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                            </tr>
                            <tr>
                                <td><strong>Total number of transfers settled:</strong></td>
                                <td>{dailySummary.totalTransfersSettled || 0}</td>
                                <td>₹{parseFloat(dailySummary.totalAmountSettled || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                            </tr>
                            <tr>
                                <td><strong>Number of fraud alerts raised:</strong></td>
                                <td colSpan="2">{dailySummary.fraudAlertsRaised || 0}</td>
                            </tr>
                            <tr>
                                <td><strong>Number of new user registrations:</strong></td>
                                <td colSpan="2">{dailySummary.newUserRegistrations || 0}</td>
                            </tr>
                            <tr>
                                <td><strong>Total pending settlement amount:</strong></td>
                                <td colSpan="2">₹{parseFloat(dailySummary.pendingSettlementAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            )}
            
            {/* Reconciliation Status from Job */}
            {!loading && dailySummary && (
                <div className={`reconciliation-status ${dailySummary.reconciliationBalanced ? 'balanced' : 'unbalanced'}`}>
                    <h4>Reconciliation Status</h4>
                    <div className="recon-status-grid">
                        <div className="recon-card">
                            <span className="recon-label">Total Debits</span>
                            <span className="recon-value">₹{parseFloat(dailySummary.totalDebits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                            <small>{dailySummary.debitCount || 0} entries</small>
                        </div>
                        <div className="recon-card">
                            <span className="recon-label">Total Credits</span>
                            <span className="recon-value">₹{parseFloat(dailySummary.totalCredits || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                            <small>{dailySummary.creditCount || 0} entries</small>
                        </div>
                        <div className={`recon-card status ${dailySummary.reconciliationBalanced ? 'success' : 'error'}`}>
                            <span className="recon-label">STATUS</span>
                            <span className="recon-status-value">
                                {dailySummary.reconciliationBalanced ? '✓ BALANCED' : '⚠️ MISMATCH DETECTED'}
                            </span>
                            {!dailySummary.reconciliationBalanced && (
                                <div className="mismatch-details">
                                    <strong>Mismatch Amount:</strong>
                                    <span className="mismatch-value">₹{parseFloat(dailySummary.reconciliationMismatch || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                                </div>
                            )}
                        </div>
                    </div>
                    <p className="last-updated">
                        Last updated: {new Date(dailySummary.updatedAt).toLocaleString('en-IN')}
                    </p>
                </div>
            )}
        </div>
    );
};

export default ReconciliationReport;
