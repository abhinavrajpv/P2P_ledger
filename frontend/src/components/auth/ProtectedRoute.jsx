import { Navigate, Outlet } from 'react-router-dom';
import { getToken, getUser } from '../../auth';

const ProtectedRoute = ({ children, requireAdmin }) => {
    const token = getToken();
    const user = getUser();

    if (!token || !user) {
        return <Navigate to="/login" replace />;
    }

    if (requireAdmin && user.role !== 'ADMIN') {
        return <Navigate to="/wallet" replace />;
    }

    return children || <Outlet />;
};

export default ProtectedRoute;
