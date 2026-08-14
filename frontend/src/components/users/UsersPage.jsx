import { useEffect, useState } from "react";
import { createUser, getUsers, updateUser } from "../../api/userApi";

const emptyForm = {
    username: "",
    displayName: "",
    password: "",
    role: "CASHIER",
    active: true,
};

export default function UsersPage() {
    const [users, setUsers] = useState([]);
    const [form, setForm] = useState(emptyForm);
    const [editingId, setEditingId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    async function loadUsers() {
        try {
            setLoading(true);
            setError("");
            setUsers(await getUsers());
        } catch (loadError) {
            setError(loadError.message || "Could not load users");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadUsers();
    }, []);

    function updateField(name, value) {
        setForm((current) => ({ ...current, [name]: value }));
    }

    function startEdit(user) {
        setEditingId(user.id);
        setForm({
            username: user.username,
            displayName: user.displayName,
            password: "",
            role: user.role,
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
                })
                : await createUser({
                    username: form.username,
                    displayName: form.displayName,
                    password: form.password,
                    role: form.role,
                });
            setUsers((current) => {
                const next = editingId
                    ? current.map((item) => item.id === saved.id ? saved : item)
                    : [...current, saved];
                return next.sort((a, b) => a.username.localeCompare(b.username));
            });
            setMessage(`${saved.displayName} was saved successfully.`);
            resetForm();
        } catch (saveError) {
            setError(saveError.message || "Could not save user");
        } finally {
            setSaving(false);
        }
    }

    return (
        <div className="feature-page">
            <div className="feature-page-header">
                <div>
                    <span className="page-label">ADMIN · ACCESS CONTROL</span>
                    <h1>Users & roles</h1>
                    <p>Create staff accounts and control their backend permissions.</p>
                </div>
                <button type="button" className="refresh-button" onClick={loadUsers}>Refresh</button>
            </div>

            {message && <div className="success-banner">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <div className="feature-columns">
                <section className="feature-card">
                    <div className="feature-card-heading">
                        <div>
                            <span className="page-label">{editingId ? "EDIT ACCOUNT" : "NEW ACCOUNT"}</span>
                            <h2>{editingId ? "Update user" : "Add user"}</h2>
                        </div>
                        {editingId && <button type="button" className="text-button" onClick={resetForm}>Cancel</button>}
                    </div>
                    <form className="feature-form" onSubmit={handleSubmit}>
                        {!editingId && (
                            <label>Username<input value={form.username} onChange={(event) => updateField("username", event.target.value)} required minLength={3} /></label>
                        )}
                        {editingId && <div className="form-readonly">Username <strong>{form.username}</strong></div>}
                        <label>Display name<input value={form.displayName} onChange={(event) => updateField("displayName", event.target.value)} required /></label>
                        <label>Password<input type="password" value={form.password} onChange={(event) => updateField("password", event.target.value)} placeholder={editingId ? "Leave blank to keep current password" : "At least 8 characters"} required={!editingId} minLength={editingId ? undefined : 8} /></label>
                        <label>Role<select value={form.role} onChange={(event) => updateField("role", event.target.value)}><option value="CASHIER">Cashier</option><option value="MANAGER">Manager</option><option value="ADMIN">Admin</option></select></label>
                        {editingId && <label className="checkbox-label"><input type="checkbox" checked={form.active} onChange={(event) => updateField("active", event.target.checked)} /> Account active</label>}
                        <button type="submit" className="product-add-button" disabled={saving}>{saving ? "Saving…" : editingId ? "Save changes" : "Create user"}</button>
                    </form>
                </section>

                <section className="feature-card feature-card-wide">
                    <div className="feature-card-heading"><div><span className="page-label">STAFF DIRECTORY</span><h2>Accounts</h2></div><span className="feature-count">{users.length} users</span></div>
                    {loading ? <p>Loading users…</p> : users.length === 0 ? <p>No users found.</p> : (
                        <div className="feature-table-wrap"><table className="feature-table"><thead><tr><th>User</th><th>Role</th><th>Status</th><th>Last login</th><th /></tr></thead><tbody>
                            {users.map((user) => <tr key={user.id}><td><strong>{user.displayName}</strong><small>@{user.username}</small></td><td><span className={`role-badge role-${user.role.toLowerCase()}`}>{user.role}</span></td><td><span className={user.active ? "status-positive" : "status-negative"}>{user.active ? "ACTIVE" : "DISABLED"}</span></td><td>{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : "Never"}</td><td><button type="button" className="text-button" onClick={() => startEdit(user)}>Edit</button></td></tr>)}
                        </tbody></table></div>
                    )}
                </section>
            </div>
        </div>
    );
}
