import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { apiFetch, ensureCsrf, readApiResponse } from "../api/http";
import { getCurrentShift } from "../api/shiftApi";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [currentShift, setCurrentShift] = useState(null);
    const [loading, setLoading] = useState(true);

    const refreshCurrentShift = useCallback(async () => {
        try {
            const shift = await getCurrentShift();
            setCurrentShift(shift);
            return shift;
        } catch (error) {
            if (error.status !== 401) console.error(error);
            setCurrentShift(null);
            return null;
        }
    }, []);

    const loadCurrentUser = useCallback(async () => {
        try {
            await ensureCsrf();
            const response = await apiFetch("/auth/me", { cache: "no-store" });
            if (response.status === 401) {
                setUser(null);
                setCurrentShift(null);
                return;
            }
            setUser(await readApiResponse(response));
            await refreshCurrentShift();
        } catch (error) {
            if (error.status !== 401) console.error(error);
            setUser(null);
            setCurrentShift(null);
        } finally {
            setLoading(false);
        }
    }, [refreshCurrentShift]);

    useEffect(() => {
        loadCurrentUser();
    }, [loadCurrentUser]);

    const login = useCallback(async (username, password) => {
        await ensureCsrf();
        const response = await apiFetch("/auth/login", {
            method: "POST",
            body: JSON.stringify({ username, password }),
        });
        const loggedInUser = await readApiResponse(response);
        setUser(loggedInUser);
        await refreshCurrentShift();
        return loggedInUser;
    }, [refreshCurrentShift]);

    const logout = useCallback(async () => {
        await readApiResponse(await apiFetch("/auth/logout", { method: "POST" }));
        setCurrentShift(null);
        setUser(null);
    }, []);

    const value = useMemo(() => ({
        user,
        currentShift,
        loading,
        login,
        logout,
        refreshCurrentShift,
        hasRole: (...roles) => Boolean(user && roles.includes(user.role)),
        hasPermission: (...permissions) => Boolean(
            user && permissions.some((permission) => (user.permissions || []).includes(permission))
        ),
    }), [user, currentShift, loading, login, logout, refreshCurrentShift]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const value = useContext(AuthContext);
    if (!value) throw new Error("useAuth must be used inside AuthProvider");
    return value;
}
