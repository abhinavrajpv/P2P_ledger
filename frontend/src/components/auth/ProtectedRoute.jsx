import { Navigate, Outlet } from 'react-router-dom';

const ProtectedRoute = ({ children, requireAdmin }) => {
    const token = localStorage.getItem('token');
    const user = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')) : null;

    if (!token || !user) {
        return <Navigate to="/login" replace />;
    }

    if (requireAdmin && user.role !== 'ADMIN') {
        return <Navigate to="/wallet" replace />;
    }

    return children || <Outlet />;
};

export default ProtectedRoute;
