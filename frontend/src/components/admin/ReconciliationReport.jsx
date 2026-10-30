import { useState, useEffect } from 'react';
import axios from 'axios';

const ReconciliationReport = () => {
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(false);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    const fetchReport = async () => {
        if (!startDate || !endDate) {
            alert('Please select start and end dates');
            return;
        }
        setLoading(true);
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/admin/reconciliation', {
                headers: { Authorization: `Bearer ${token}` },
                params: { startDate, endDate }
            });
            setReports(response.data);
        } catch (error) {
            console.error('Failed to fetch reconciliation report');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="reconciliation-report">
            <h3>Daily Reconciliation Report</h3>
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
                <p>Select dates to view reconciliation report</p>
            )}
            {!loading && reports.length > 0 && (
                <div className="reports-list">
                    {reports.map(report => (
                        <div key={report.id} className="report-item">
                            <div className="report-header">
                                <span className="summary-date">Date: {report.summaryDate}</span>
                                <span className="created-at">Created: {new Date(report.createdAt).toLocaleDateString()}</span>
                            </div>
                            <div className="report-stats">
                                <span>Total Transfers: {report.totalTransfers}</span>
                                <span>Total Amount: ₹{parseFloat(report.totalAmount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                                <span>Successful: {report.successfulTransfers}</span>
                                <span>Failed: {report.failedTransfers}</span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ReconciliationReport;
