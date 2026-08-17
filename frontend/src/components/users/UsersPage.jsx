import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";
import { createUser, deleteUser, getUsers, setUserActive, updateUser } from "../../api/userApi";
import { getRules } from "../../api/roleApi.js";

const emptyForm = {
    username: "",
    displayName: "",
    password: "",
    role: "CASHIER",
    ruleId: "",
    active: true,
};

function translatedEnum(t, prefix, value) {
    if (!value) return t("common.dash");
    const key = prefix + "." + value;
    const translated = t(key);
    return translated === key ? value : translated;
}

function canAssignRule(rule, role) {
    if (!rule) return false;
    if (rule.systemRule && rule.systemRole !== role) return false;
    return role !== "ADMIN" || (rule.systemRule && rule.systemRole === "ADMIN");
}

export default function UsersPage() {
    const { t, language } = useLanguage();
    const { user: currentUser } = useAuth();
    const [users, setUsers] = useState([]);
    const [rules, setRules] = useState([]);
    const [form, setForm] = useState(emptyForm);
    const [editingId, setEditingId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [busyId, setBusyId] = useState(null);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const roleLabel = (role) => translatedEnum(t, "roles.role", role);
    const dateTime = (value) => value
        ? new Date(value).toLocaleString(language === "ar" ? "ar-EG" : "en-EG")
        : t("users.never");
    const ruleName = (rule) => rule.systemRule && rule.systemRole
        ? roleLabel(rule.systemRole)
        : (rule.name || t("common.rule"));
    const profileName = (name, role) => ["ADMIN", "MANAGER", "CASHIER"].includes(name)
        ? roleLabel(name)
        : (name || roleLabel(role));

    async function loadUsers() {
        try {
            setLoading(true);
            setError("");
            const [loadedUsers, loadedRules] = await Promise.all([getUsers(), getRules()]);
            setUsers(loadedUsers);
            setRules(loadedRules);
        } catch (loadError) {
            setError(loadError.message || t("users.loadError"));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadUsers();
    }, []);

    function updateField(name, value) {
        setForm((current) => {
            const next = { ...current, [name]: value };
            if (name === "role" && current.ruleId) {
                const selectedRule = rules.find((rule) => String(rule.id) === String(current.ruleId));
                if (!canAssignRule(selectedRule, value)) {
                    next.ruleId = "";
                }
            }
            return next;
        });
    }

    function startEdit(user) {
        setEditingId(user.id);
        setForm({
            username: user.username,
            displayName: user.displayName,
            password: "",
            role: user.role,
            ruleId: user.ruleId ? String(user.ruleId) : "",
            active: user.active,
        });
        setMessage("");
        setError("");
    }

    function resetForm() {
        setEditingId(null);
        setForm(emptyForm);
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setSaving(true);
        setMessage("");
        setError("");
        try {
            const saved = editingId
                ? await updateUser(editingId, {
                    displayName: form.displayName,
                    password: form.password || null,
                    role: form.role,
                    active: form.active,
                    ruleId: form.ruleId ? Number(form.ruleId) : null,
                })
                : await createUser({
                    username: form.username,
                    displayName: form.displayName,
                    password: form.password,
                    role: form.role,
                    ruleId: form.ruleId ? Number(form.ruleId) : null,
                });
            setUsers((current) => {
                const next = editingId
                    ? current.map((item) => item.id === saved.id ? saved : item)
                    : [...current, saved];
                return next.sort((a, b) => a.username.localeCompare(b.username));
            });
            setMessage(t("users.saved", { name: saved.displayName }));
            resetForm();
        } catch (saveError) {
            setError(saveError.message || t("users.saveError"));
        } finally {
            setSaving(false);
        }
    }

    async function handleToggleActive(user) {
        try {
            setBusyId(user.id);
            setMessage("");
            setError("");
            const saved = await setUserActive(user.id, !user.active);
            setUsers((current) => current.map((item) => item.id === saved.id ? saved : item));
            if (editingId === saved.id) {
                setForm((current) => ({ ...current, active: saved.active }));
            }
            setMessage(t(saved.active ? "users.activated" : "users.deactivated", { name: saved.displayName }));
        } catch (statusError) {
            setError(statusError.message || t("users.statusError"));
        } finally {
            setBusyId(null);
        }
    }

    async function handleDelete(user) {
        if (!window.confirm(t("users.deleteConfirm", { name: user.displayName }))) return;

        try {
            setBusyId(user.id);
            setMessage("");
            setError("");
            await deleteUser(user.id);
            setUsers((current) => current.filter((item) => item.id !== user.id));
            if (editingId === user.id) resetForm();
            setMessage(t("users.deleted", { name: user.displayName }));
        } catch (deleteError) {
            setError(deleteError.message || t("users.deleteError"));
        } finally {
            setBusyId(null);
        }
    }

    return (
        <div className="feature-page">
            <div className="feature-page-header">
                <div>
                    <span className="page-label">{t("users.pageLabel")}</span>
                    <h1>{t("users.title")}</h1>
                    <p>{t("users.description")}</p>
                </div>
            </div>

            {message && <div className="success-banner">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <div className="feature-columns">
                <section className="feature-card">
                    <div className="feature-card-heading">
                        <div>
                            <span className="page-label">{editingId ? t("users.editAccount") : t("users.newAccount")}</span>
                            <h2>{editingId ? t("users.updateUser") : t("users.addUser")}</h2>
                        </div>
                        {editingId && <button type="button" className="text-button" onClick={resetForm}>{t("common.cancel")}</button>}
                    </div>
                    <form className="feature-form" onSubmit={handleSubmit}>
                        {!editingId && (
                            <label>
                                {t("users.username")}
                                <input value={form.username} onChange={(event) => updateField("username", event.target.value)} required minLength={3} />
                            </label>
                        )}
                        {editingId && <div className="form-readonly">{t("users.username")} <strong>{form.username}</strong></div>}
                        <label>
                            {t("users.displayName")}
                            <input value={form.displayName} onChange={(event) => updateField("displayName", event.target.value)} required />
                        </label>
                        <label>
                            {t("users.password")}
                            <input type="password" value={form.password} onChange={(event) => updateField("password", event.target.value)} placeholder={editingId ? t("users.keepPassword") : t("users.newPassword")} required={!editingId} minLength={editingId ? undefined : 8} />
                        </label>
                        <label>
                            {t("users.baseRole")}
                            <select value={form.role} onChange={(event) => updateField("role", event.target.value)}>
                                <option value="CASHIER">{roleLabel("CASHIER")}</option>
                                <option value="MANAGER">{roleLabel("MANAGER")}</option>
                                <option value="ADMIN">{roleLabel("ADMIN")}</option>
                            </select>
                        </label>
                        <label>
                            {t("users.accessProfile")}
                            <select value={form.ruleId} onChange={(event) => updateField("ruleId", event.target.value)}>
                                <option value="">{t("users.defaultRole", { role: roleLabel(form.role) })}</option>
                                {rules
                                    .filter((rule) => !rule.systemRule || rule.systemRole === form.role)
                                    .sort((a, b) => Number(Boolean(a.systemRule)) - Number(Boolean(b.systemRule)))
                                    .map((rule) => (
                                        <option key={rule.id} value={rule.id} disabled={!canAssignRule(rule, form.role)}>
                                            {ruleName(rule)} · {rule.systemRule ? t("common.builtIn") : t("common.custom")}
                                        </option>
                                    ))}
                            </select>
                            <small className="form-help">
                                {form.role === "ADMIN" ? t("users.customRoleAdminNote") : t("users.roleHelp")}
                            </small>
                        </label>
                        {editingId && (
                            <label className="checkbox-label">
                                <input type="checkbox" checked={form.active} disabled={currentUser?.id === editingId} onChange={(event) => updateField("active", event.target.checked)} />
                                {t("users.accountActive")}
                            </label>
                        )}
                        <button type="submit" className="product-add-button" disabled={saving}>
                            {saving ? t("users.saving") : editingId ? t("users.saveChanges") : t("users.createUser")}
                        </button>
                    </form>
                </section>

                <section className="feature-card feature-card-wide">
                    <div className="feature-card-heading">
                        <div><span className="page-label">{t("users.staffDirectory")}</span><h2>{t("users.accounts")}</h2></div>
                        <span className="feature-count">{t("users.count", { count: users.length })}</span>
                    </div>
                    {loading ? <p>{t("users.loading")}</p> : users.length === 0 ? <p>{t("users.noUsers")}</p> : (
                        <div className="feature-table-wrap">
                            <table className="feature-table">
                                <thead>
                                    <tr><th>{t("users.user")}</th><th>{t("users.role")}</th><th>{t("users.rule")}</th><th>{t("common.status")}</th><th>{t("users.lastLogin")}</th><th /></tr>
                                </thead>
                                <tbody>
                                    {users.map((user) => {
                                        const busy = busyId === user.id;
                                        const isCurrentUser = currentUser?.username === user.username;
                                        return (
                                            <tr key={user.id}>
                                                <td><strong>{user.displayName}</strong><small>@{user.username}</small></td>
                                                <td><span className={"role-badge role-" + user.role.toLowerCase()}>{roleLabel(user.role)}</span></td>
                                                <td><span className="status-muted">{profileName(user.ruleName, user.role)}</span></td>
                                                <td><span className={user.active ? "status-positive" : "status-negative"}>{user.active ? t("common.active") : t("common.disabled")}</span></td>
                                                <td>{dateTime(user.lastLoginAt)}</td>
                                                <td>
                                                    <div className="user-row-actions">
                                                        <button type="button" className="text-button" onClick={() => startEdit(user)} disabled={busy}>{t("common.edit")}</button>
                                                        <button
                                                            type="button"
                                                            className="text-button"
                                                            onClick={() => handleToggleActive(user)}
                                                            disabled={busy || isCurrentUser}
                                                            title={isCurrentUser ? t("users.cannotChangeStatus") : undefined}
                                                        >
                                                            {busy ? t("common.working") : user.active ? t("users.deactivate") : t("users.activate")}
                                                        </button>
                                                        <button
                                                            type="button"
                                                            className="product-delete-button"
                                                            onClick={() => handleDelete(user)}
                                                            disabled={busy || isCurrentUser}
                                                            title={isCurrentUser ? t("users.cannotDelete") : undefined}
                                                        >
                                                            {t("common.delete")}
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}
