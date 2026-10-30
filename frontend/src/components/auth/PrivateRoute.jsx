import { Navigate, Outlet } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { getToken, getUser } from '../../auth';

const PrivateRoute = ({ children, requireAdmin }) => {
    const [token, setToken] = useState(null);
    const [user, setUser] = useState(null);
    const [checked, setChecked] = useState(false);

    useEffect(() => {
        const storedToken = getToken();
        const storedUser = getUser();
        if (storedToken) setToken(storedToken);
        if (storedUser) setUser(storedUser);
        setChecked(true);
    }, []);

    if (!checked) return <div className="loading">Loading...</div>;
    if (!token || !user) return <Navigate to="/login" replace />;
    if (requireAdmin && user.role !== 'ADMIN') return <Navigate to="/wallet" replace />;

    return children || <Outlet />;
};

export default PrivateRoute;
