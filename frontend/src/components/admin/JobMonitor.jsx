import { useState, useEffect } from 'react';
import axios from 'axios';

const JobMonitor = () => {
    const [jobs, setJobs] = useState([]);
    const [loading, setLoading] = useState(false);
    const [jobName, setJobName] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchJobs = async (page = 0) => {
        setLoading(true);
        try {
            const params = {
                page,
                size: 10,
                jobName: jobName || undefined
            };
            const response = await axios.get('/api/admin/jobs', {
                params
            });
            setJobs(response.data);
            setTotalPages(response.data.totalPages || 1);
            setPage(page);
        } catch (error) {
            console.error('Failed to fetch job logs:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchJobs();
    }, []);

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchJobs(newPage);
        }
    };

    return (
        <div className="job-monitor">
            <h3>Scheduled Job Monitor</h3>
            <div className="job-filters">
                <input
                    type="text"
                    placeholder="Filter by job name"
                    value={jobName}
                    onChange={(e) => setJobName(e.target.value)}
                />
                <button onClick={() => fetchJobs(0)}>Search</button>
            </div>
            {loading && <div className="loading">Loading jobs...</div>}
            {!loading && jobs.length === 0 && <p>No job logs found</p>}
            {!loading && jobs.length > 0 && (
                <>
                    <div className="jobs-list">
                        {jobs.map(job => (
                            <div key={job.id} className="job-item">
                                <div className="job-info">
                                    <span className="job-name">{job.jobName}</span>
                                    <span className={`status-${job.status?.toLowerCase()}`}>{job.status}</span>
                                    <span className="job-time">{new Date(job.executionTime).toLocaleString()}</span>
                                </div>
                                <div className="job-details">{job.details || 'No details'}</div>
                            </div>
                        ))}
                    </div>
                    <div className="pagination">
                        <button onClick={() => handlePageChange(page - 1)} disabled={page === 0}>Previous</button>
                        <span>Page {page + 1} of {totalPages}</span>
                        <button onClick={() => handlePageChange(page + 1)} disabled={page >= totalPages - 1}>Next</button>
                    </div>
                </>
            )}
        </div>
    );
};

export default JobMonitor;
