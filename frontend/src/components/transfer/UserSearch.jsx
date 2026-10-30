import { useState, useEffect } from 'react';
import axios from 'axios';

const UserSearch = ({ onUserSelect }) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (query.length < 2) {
            setResults([]);
            return;
        }
        const timeoutId = setTimeout(() => {
            fetchUsers();
        }, 300);
        return () => clearTimeout(timeoutId);
    }, [query]);

    const fetchUsers = async () => {
        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/users/search', {
                headers: { Authorization: `Bearer ${token}` },
                params: { query }
            });
            setResults(response.data);
        } catch (error) {
            setError('Failed to search users');
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="user-search">
            <h3>Search User</h3>
            <div className="search-input">
                <input
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search by email or phone..."
                />
            </div>
            {error && <div className="error">{error}</div>}
            {loading && <div className="loading">Searching...</div>}
            {!loading && results.length > 0 && (
                <div className="search-results">
                    {results.map(user => (
                        <div
                            key={user.id}
                            className="search-result"
                            onClick={() => onUserSelect?.(user)}
                        >
                            <span className="user-name">{user.name}</span>
                            <span className="user-email">{user.email}</span>
                            <span className="user-phone">{user.phone}</span>
                        </div>
                    ))}
                </div>
            )}
            {!loading && query.length > 1 && results.length === 0 && (
                <div className="no-results">No users found</div>
            )}
        </div>
    );
};

export default UserSearch;
