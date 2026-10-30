import { useState, useEffect } from 'react';
import axios from 'axios';

const ReconciliationReport = () => {
    const [reports, setReports] = useState([]);
    const [todayRecon, setTodayRecon] = useState(null);
    const [loading, setLoading] = useState(false);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    const fetchTodayReconciliation = async () => {
        try {
            const response = await axios.get('/api/admin/reconciliation/today');
            setTodayRecon(response.data);
        } catch (error) {
            console.error('Failed to fetch today\'s reconciliation:', error.response?.data?.message || error.message);
        }
    };

    const fetchReport = async () => {
        if (!startDate || !endDate) {
            alert('Please select start and end dates');
            return;
        }
        setLoading(true);
        try {
            const response = await axios.get('/api/admin/reconciliation', {
                params: { startDate, endDate }
            });
            setReports(response.data);
        } catch (error) {
            console.error('Failed to fetch reconciliation report:', error.response?.data?.message || error.message);
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
            
            {/* Today's Reconciliation Status */}
            {todayRecon && (
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
                        <div className={`recon-card status ${todayRecon.balanced ? 'success' : 'error'}`}>
                            <span className="recon-label">Status</span>
                            <span className="recon-status-value">
                                {todayRecon.balanced ? '✓ Balanced' : '⚠️ Mismatch Detected'}
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

            {/* Historical Report Section */}
            <div className="historical-report">
                <h4>Historical Reconciliation Report</h4>
                <div className="date-filters">
                    <input
                        type="date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        placeholder="Start Date"
                    />
                    <input
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        placeholder="End Date"
                    />
                    <button onClick={fetchReport} disabled={loading}>
                        {loading ? 'Loading...' : 'Generate Report'}
                    </button>
                </div>
                {!loading && reports.length === 0 && !startDate && (
                    <p>Select dates to view historical reconciliation report</p>
                )}
                {!loading && reports.length > 0 && (
                    <div className="reports-list">
                        {reports.map(report => (
                            <div key={report.id} className="report-item">
                                <div className="report-header">
                                    <span className="summary-date">Date: {report.summaryDate}</span>
                                    <span className="created-at">Generated: {new Date(report.createdAt).toLocaleDateString('en-IN')}</span>
                                </div>
                                <div className="report-stats">
                                    <span>Total Transfers: {report.totalTransfers}</span>
                                    <span>Total Amount: ₹{parseFloat(report.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                                    <span className="success-count">Successful: {report.successfulTransfers}</span>
                                    <span className="failed-count">Failed: {report.failedTransfers}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ReconciliationReport;
