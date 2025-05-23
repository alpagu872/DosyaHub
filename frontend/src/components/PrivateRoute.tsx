import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { authService } from '../services/authService';

interface PrivateRouteProps {
    children: React.ReactNode;
}

const PrivateRoute: React.FC<PrivateRouteProps> = ({ children }) => {
    const { token } = useSelector((state: RootState) => state.auth);
    
    if (!authService.isAuthenticated()) {
        return <Navigate to="/login" />;
    }

    return <>{children}</>;
};

export default PrivateRoute; 