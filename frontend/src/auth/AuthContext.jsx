import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { apiFetch, ensureCsrf, readApiResponse } from "../api/http";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    const loadCurrentUser = useCallback(async () => {
        try {
            await ensureCsrf();
            const response = await apiFetch("/auth/me", { cache: "no-store" });
            if (response.status === 401) {
                setUser(null);
                return;
            }
            setUser(await readApiResponse(response));
        } catch (error) {
            if (error.status !== 401) console.error(error);
            setUser(null);
        } finally {
            setLoading(false);
        }
    }, []);

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
        return loggedInUser;
    }, []);

    const logout = useCallback(async () => {
        try {
            await readApiResponse(await apiFetch("/auth/logout", { method: "POST" }));
        } finally {
            setUser(null);
        }
    }, []);

    const value = useMemo(() => ({
        user,
        loading,
        login,
        logout,
        hasRole: (...roles) => Boolean(user && roles.includes(user.role)),
    }), [user, loading, login, logout]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const value = useContext(AuthContext);
    if (!value) throw new Error("useAuth must be used inside AuthProvider");
    return value;
}
