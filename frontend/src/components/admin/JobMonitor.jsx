import { useState, useEffect } from 'react';
import axios from 'axios';

const JobMonitor = () => {
    const [jobs, setJobs] = useState([]);
    const [loading, setLoading] = useState(false);
    const [jobName, setJobName] = useState('');

    const fetchJobs = async () => {
        setLoading(true);
        try {
            const params = {
                jobName: jobName || undefined
            };
            const response = await axios.get('/api/admin/jobs', {
                params
            });
            // The backend now returns only the last 20 runs
            const jobsList = Array.isArray(response.data) ? response.data : [];
            setJobs(jobsList);
        } catch (error) {
            console.error('Failed to fetch job logs:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    const parseJobDetails = (detailsString) => {
        try {
            const parsed = JSON.parse(detailsString);
            return parsed;
        } catch {
            return null;
        }
    };

    const formatJobName = (name) => {
        return name
            .split('_')
            .map(word => word.charAt(0).toUpperCase() + word.slice(1))
            .join(' ');
    };

    const isSettlementJob = (jobName) => {
        return jobName && jobName.toLowerCase().includes('settlement');
    };

    useEffect(() => {
        fetchJobs();
        // Auto-refresh every 30 seconds
        const interval = setInterval(fetchJobs, 30000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="job-monitor">
            <div className="monitor-header">
                <h3>Scheduled Job Monitor</h3>
                <small className="last-runs-info">Showing last 20 job executions</small>
            </div>
            <div className="job-filters">
                <input
                    type="text"
                    placeholder="Filter by job name (e.g., SETTLEMENT)"
                    value={jobName}
                    onChange={(e) => setJobName(e.target.value)}
                />
                <button onClick={fetchJobs}>Search</button>
                <button onClick={() => { setJobName(''); fetchJobs(); }} className="clear-btn">
                    Clear
                </button>
            </div>
            {loading && <div className="loading">Loading jobs...</div>}
            {!loading && jobs.length === 0 && <p>No job logs found</p>}
            {!loading && jobs.length > 0 && (
                <>
                    <div className="jobs-list">
                        {jobs.map(job => {
                            const details = parseJobDetails(job.details);
                            const isSettlement = isSettlementJob(job.jobName);
                            return (
                                <div key={job.id} className={`job-log ${isSettlement ? 'settlement-job' : ''}`}>
                                    <div className="job-header">
                                        <div className="job-name-section">
                                            {isSettlement && <span className="settlement-badge">💰 Settlement</span>}
                                            <span className="job-name-label">{formatJobName(job.jobName)}</span>
                                            <span className={`job-status ${job.status?.toLowerCase()}`}>
                                                {job.status}
                                            </span>
                                        </div>
                                        <span className="job-time">
                                            {new Date(job.executionTime).toLocaleString('en-IN')}
                                        </span>
                                    </div>
                                    
                                    {details && (
                                        <div className="job-details">
                                            {details.batchId && (
                                                <div className="detail-row highlight">
                                                    <span className="detail-label">Batch ID:</span>
                                                    <span className="detail-value">{details.batchId}</span>
                                                </div>
                                            )}
                                            
                                            {/* Settlement Job Stats - Highlighted */}
                                            {isSettlement && (
                                                <>
                                                    {details.processedCount !== undefined && (
                                                        <div className="detail-row settlement-stat success">
                                                            <span className="detail-label">✅ Settled Count:</span>
                                                            <span className="detail-value">{details.processedCount}</span>
                                                        </div>
                                                    )}
                                                    {details.failureCount !== undefined && (
                                                        <div className={`detail-row settlement-stat ${details.failureCount > 0 ? 'error' : ''}`}>
                                                            <span className="detail-label">❌ Failed Count:</span>
                                                            <span className="detail-value">{details.failureCount}</span>
                                                        </div>
                                                    )}
                                                    {details.totalAmount !== undefined && (
                                                        <div className="detail-row settlement-stat highlight">
                                                            <span className="detail-label">💵 Total Amount:</span>
                                                            <span className="detail-value">₹{details.totalAmount}</span>
                                                        </div>
                                                    )}
                                                </>
                                            )}
                                            
                                            {/* Other job stats */}
                                            {!isSettlement && details.processedCount !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Processed:</span>
                                                    <span className="detail-value">{details.processedCount}</span>
                                                </div>
                                            )}
                                            {!isSettlement && details.failureCount !== undefined && (
                                                <div className={`detail-row ${details.failureCount > 0 ? 'error' : ''}`}>
                                                    <span className="detail-label">Failures:</span>
                                                    <span className="detail-value">{details.failureCount}</span>
                                                </div>
                                            )}
                                            
                                            {details.accountsChecked !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Accounts Checked:</span>
                                                    <span className="detail-value">{details.accountsChecked}</span>
                                                </div>
                                            )}
                                            {details.accountsUnlocked !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Accounts Unlocked:</span>
                                                    <span className="detail-value">{details.accountsUnlocked}</span>
                                                </div>
                                            )}
                                            {details.walletsProcessed !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Wallets Processed:</span>
                                                    <span className="detail-value">{details.walletsProcessed}</span>
                                                </div>
                                            )}
                                            {details.todaySpendingReset !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Spending Reset:</span>
                                                    <span className="detail-value">{details.todaySpendingReset}</span>
                                                </div>
                                            )}
                                            {details.totalTransfers !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Total Transfers:</span>
                                                    <span className="detail-value">{details.totalTransfers}</span>
                                                </div>
                                            )}
                                            {details.successfulTransfers !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Successful:</span>
                                                    <span className="detail-value">{details.successfulTransfers}</span>
                                                </div>
                                            )}
                                            {details.failedTransfers !== undefined && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Failed:</span>
                                                    <span className="detail-value">{details.failedTransfers}</span>
                                                </div>
                                            )}
                                            {!isSettlement && details.totalAmount !== undefined && (
                                                <div className="detail-row highlight">
                                                    <span className="detail-label">Total Amount:</span>
                                                    <span className="detail-value">₹{details.totalAmount}</span>
                                                </div>
                                            )}
                                            {details.error && (
                                                <div className="detail-row error">
                                                    <span className="detail-label">Error:</span>
                                                    <span className="detail-value">{details.error}</span>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                    
                                    {!details && job.details && (
                                        <div className="job-details">
                                            <div className="detail-row">
                                                <span className="detail-value">{job.details}</span>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </>
            )}
        </div>
    );
};

export default JobMonitor;
