import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../../i18n";
import { createRule, deleteRule, getRules, updateRule } from "../../api/ruleApi";
import { getPermissions } from "../../api/permissionApi";
import DeleteRoleModal from "./DeleteRoleModal";

const emptyForm = {
    name: "",
    description: "",
    systemRule: false,
    systemRole: null,
};

function translatedEnum(t, prefix, value) {
    if (!value) return t("common.dash");
    const key = prefix + "." + value;
    const translated = t(key);
    return translated === key ? value : translated;
}

function permissionText(t, permission, field) {
    const key = "permissions.item." + permission.code + "." + field;
    const translated = t(key);
    return translated === key ? permission[field] : translated;
}

function categoryText(t, category) {
    const key = "permissions.category." + String(category || "OTHER").replaceAll(" ", "_");
    const translated = t(key);
    return translated === key ? category || "OTHER" : translated;
}

export default function RolesPage() {
    const { t } = useLanguage();
    const [rules, setRules] = useState([]);
    const [catalog, setCatalog] = useState([]);
    const [form, setForm] = useState(emptyForm);
    const [selectedCodes, setSelectedCodes] = useState(new Set());
    const [editingId, setEditingId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [busyId, setBusyId] = useState(null);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [deleteTarget, setDeleteTarget] = useState(null);
    const [deleteError, setDeleteError] = useState("");

    const roleLabel = (role) => translatedEnum(t, "roles.role", role);
    const ruleName = (rule) => rule.systemRule && rule.systemRole
        ? roleLabel(rule.systemRole)
        : (rule.name || t("roles.roleFallback"));
    const ruleDescription = (rule) => rule.systemRule && rule.systemRole
        ? t("roles.systemDescription." + rule.systemRole)
        : (rule.description || t("roles.noDescription"));

    useEffect(() => {
        loadData();
    }, []);

    async function loadData() {
        try {
            setLoading(true);
            setError("");
            const [loadedRules, loadedCatalog] = await Promise.all([
                getRules(),
                getPermissions(),
            ]);
            setRules(loadedRules);
            setCatalog(loadedCatalog);
        } catch (loadError) {
            setError(loadError.message || t("roles.loadError"));
        } finally {
            setLoading(false);
        }
    }

    function updateField(name, value) {
        setForm((current) => ({ ...current, [name]: value }));
    }

    function startCreate() {
        setEditingId(null);
        setForm(emptyForm);
        setSelectedCodes(new Set());
        setMessage("");
        setError("");
    }

    function startEdit(rule) {
        setEditingId(rule.id);
        setForm({
            name: rule.name,
            description: rule.description || "",
            systemRule: Boolean(rule.systemRule),
            systemRole: rule.systemRole || null,
        });
        setSelectedCodes(new Set(rule.permissions || []));
        setMessage("");
        setError("");
    }

    function togglePermission(code) {
        setSelectedCodes((current) => {
            const next = new Set(current);
            if (next.has(code)) next.delete(code);
            else next.add(code);
            return next;
        });
        setMessage("");
    }

    async function handleSubmit(event) {
        event.preventDefault();
        try {
            setSaving(true);
            setMessage("");
            setError("");
            const payload = {
                name: form.name,
                description: form.description,
                permissions: [...selectedCodes],
            };
            const saved = editingId
                ? await updateRule(editingId, payload)
                : await createRule(payload);
            setRules((current) => {
                const next = editingId
                    ? current.map((rule) => rule.id === saved.id ? saved : rule)
                    : [...current, saved];
                return next.sort((a, b) => {
                    if (Boolean(a.systemRule) !== Boolean(b.systemRule)) {
                        return a.systemRule ? -1 : 1;
                    }
                    return a.name.localeCompare(b.name);
                });
            });
            startEdit(saved);
            setMessage(t("roles.saved", { name: ruleName(saved) }));
        } catch (saveError) {
            setError(saveError.message || t("roles.saveError"));
        } finally {
            setSaving(false);
        }
    }

    function requestDelete(rule) {
        if (rule.systemRule) return;
        setDeleteError("");
        setDeleteTarget(rule);
    }

    async function handleDelete(rule) {
        try {
            setBusyId(rule.id);
            setMessage("");
            setError("");
            setDeleteError("");
            await deleteRule(rule.id);
            setRules((current) => current.filter((item) => item.id !== rule.id));
            if (editingId === rule.id) startCreate();
            setDeleteTarget(null);
            setMessage(t("roles.deleted", { name: ruleName(rule) }));
        } catch (deleteCallError) {
            const deleteMessage = deleteCallError.message || t("roles.deleteError");
            setDeleteError(deleteMessage);
            setError(deleteMessage);
        } finally {
            setBusyId(null);
        }
    }

    const groupedPermissions = useMemo(() => catalog.reduce((groups, permission) => {
        const category = permission.category || "OTHER";
        groups[category] = [...(groups[category] || []), permission];
        return groups;
    }, {}), [catalog]);

    const canManagePermissions = form.systemRule && form.systemRole === "ADMIN";

    return (
        <div className="feature-page rules-page">
            <div className="feature-page-header">
                <div>
                    <span className="page-label">{t("roles.pageLabel")}</span>
                    <h1>{t("roles.title")}</h1>
                    <p>{t("roles.description")}</p>
                </div>
                <button type="button" className="product-add-button" onClick={startCreate}>{t("roles.addRole")}</button>
            </div>

            {message && <div className="success-banner">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <div className="feature-columns rules-columns">
                <section className="feature-card">
                    <div className="feature-card-heading">
                        <div>
                            <span className="page-label">{editingId ? t("roles.editRole") : t("roles.newRole")}</span>
                            <h2>{editingId ? t("roles.updateRole") : t("roles.addRole")}</h2>
                        </div>
                        {editingId && <button type="button" className="text-button" onClick={startCreate}>{t("roles.cancel")}</button>}
                    </div>
                    <form className="feature-form" onSubmit={handleSubmit}>
                        <label>
                            {t("roles.name")}
                            <input
                                value={form.systemRule ? roleLabel(form.systemRole) : form.name}
                                onChange={(event) => updateField("name", event.target.value)}
                                disabled={form.systemRule}
                                required
                                maxLength={50}
                            />
                        </label>
                        <label>
                            {t("roles.descriptionField")}
                            <textarea
                                className="feature-textarea"
                                value={form.description}
                                onChange={(event) => updateField("description", event.target.value)}
                                maxLength={200}
                                rows={3}
                            />
                        </label>
                        <div className="rule-form-note">
                            {form.systemRule
                                ? t("roles.systemNote", { role: roleLabel(form.systemRole) })
                                : t("roles.customNote")}
                        </div>
                        <button type="submit" className="product-add-button" disabled={saving}>
                            {saving ? t("roles.saving") : editingId ? t("roles.saveChanges") : t("roles.create")}
                        </button>
                    </form>
                </section>

                <section className="feature-card feature-card-wide">
                    <div className="feature-card-heading">
                        <div>
                            <span className="page-label">{t("roles.permissionAssignment")}</span>
                            <h2>{editingId ? (form.systemRule ? roleLabel(form.systemRole) : (form.name || t("roles.roleFallback"))) + " · " + t("roles.permissions") : t("roles.choosePermissions")}</h2>
                        </div>
                        <span className="feature-count">{t("roles.selected", { count: selectedCodes.size })}</span>
                    </div>
                    {loading ? <p>{t("roles.loadingPermissions")}</p> : (
                        <>
                            {Object.entries(groupedPermissions).map(([category, permissions]) => (
                                <div className="permission-group" key={category}>
                                    <div className="permission-group-heading">{categoryText(t, category)}</div>
                                    <div className="permission-grid">
                                        {permissions.map((permission) => {
                                            const locked = permission.code === "PERMISSIONS_MANAGE" && !canManagePermissions;
                                            return (
                                                <label className={"permission-option " + (selectedCodes.has(permission.code) ? "selected " : "") + (locked ? "locked" : "")} key={permission.code}>
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedCodes.has(permission.code)}
                                                        onChange={() => togglePermission(permission.code)}
                                                        disabled={locked}
                                                    />
                                                    <span>
                                                        <strong>{permissionText(t, permission, "label")}</strong>
                                                        <small>{permissionText(t, permission, "description")}</small>
                                                        {locked && <em>{t("roles.availableOnlyAdmin")}</em>}
                                                    </span>
                                                </label>
                                            );
                                        })}
                                    </div>
                                </div>
                            ))}
                            <p className="permissions-note">{t("roles.permissionsNote")}</p>
                        </>
                    )}
                </section>
            </div>

            <section className="feature-card rules-list-card">
                <div className="feature-card-heading">
                    <div>
                        <span className="page-label">{t("roles.accessProfiles")}</span>
                        <h2>{t("roles.inSystem")}</h2>
                    </div>
                    <span className="feature-count">{t("roles.count", { count: rules.length })}</span>
                </div>
                {loading ? <p>{t("roles.loading")}</p> : rules.length === 0 ? <p>{t("roles.noRoles")}</p> : (
                    <div className="feature-table-wrap">
                        <table className="feature-table rules-table">
                            <thead>
                                <tr><th>{t("roles.role")}</th><th>{t("roles.type")}</th><th>{t("roles.users")}</th><th>{t("roles.permissions")}</th><th /></tr>
                            </thead>
                            <tbody>
                                {rules.map((rule) => {
                                    const busy = busyId === rule.id;
                                    return (
                                        <tr key={rule.id}>
                                            <td>
                                                <strong>{ruleName(rule)}</strong>
                                                <small>{ruleDescription(rule)}</small>
                                            </td>
                                            <td>
                                                <span className={rule.systemRule ? "status-muted" : "status-positive"}>
                                                    {rule.systemRule ? t("roles.builtIn") : t("roles.custom")}
                                                </span>
                                            </td>
                                            <td>{rule.userCount}</td>
                                            <td>{rule.permissions?.length || 0}</td>
                                            <td>
                                                <div className="user-row-actions">
                                                    <button type="button" className="text-button" onClick={() => startEdit(rule)} disabled={busy}>{t("common.edit")}</button>
                                                    <button
                                                        type="button"
                                                        className="product-delete-button"
                                                        onClick={() => requestDelete(rule)}
                                                        disabled={busy || rule.systemRule}
                                                        title={rule.systemRule ? t("roles.builtInCannotDelete") : undefined}
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

            {deleteTarget && (
                <DeleteRoleModal
                    rule={deleteTarget}
                    name={ruleName(deleteTarget)}
                    deleting={busyId === deleteTarget.id}
                    error={deleteError}
                    onClose={() => {
                        setDeleteTarget(null);
                        setDeleteError("");
                    }}
                    onConfirm={() => handleDelete(deleteTarget)}
                />
            )}
        </div>
    );
}
