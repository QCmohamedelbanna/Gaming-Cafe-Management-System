import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../../i18n";
import { getPermissions, updateRolePermissions } from "../../api/permissionApi";

const roles = ["ADMIN", "MANAGER", "CASHIER"];

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

export default function PermissionsPage() {
    const { t } = useLanguage();
    const [catalog, setCatalog] = useState([]);
    const [activeRole, setActiveRole] = useState("ADMIN");
    const [selectedCodes, setSelectedCodes] = useState(new Set());
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {
        loadPermissions();
    }, []);

    useEffect(() => {
        setSelectedCodes(new Set(
            catalog
                .filter((permission) => permission.roles?.includes(activeRole))
                .map((permission) => permission.code)
        ));
    }, [catalog, activeRole]);

    async function loadPermissions() {
        try {
            setLoading(true);
            setError("");
            setCatalog(await getPermissions());
        } catch (loadError) {
            setError(loadError.message || t("permissions.loadError"));
        } finally {
            setLoading(false);
        }
    }

    function changeRole(role) {
        setActiveRole(role);
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

    async function savePermissions() {
        try {
            setSaving(true);
            setMessage("");
            setError("");
            const updatedCatalog = await updateRolePermissions(activeRole, [...selectedCodes]);
            setCatalog(updatedCatalog);
            setMessage(t("permissions.saved", { role: translatedEnum(t, "roles.role", activeRole) }));
        } catch (saveError) {
            setError(saveError.message || t("permissions.saveError"));
        } finally {
            setSaving(false);
        }
    }

    const groupedPermissions = useMemo(() => catalog.reduce((groups, permission) => {
        const category = permission.category || "OTHER";
        groups[category] = [...(groups[category] || []), permission];
        return groups;
    }, {}), [catalog]);

    return (
        <div className="feature-page permissions-page">
            <div className="feature-page-header">
                <div>
                    <span className="page-label">{t("permissions.pageLabel")}</span>
                    <h1>{t("permissions.title")}</h1>
                    <p>{t("permissions.description")}</p>
                </div>
            </div>

            {message && <div className="success-banner">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <section className="feature-card permissions-role-card">
                <div className="feature-card-heading">
                    <div>
                        <span className="page-label">{t("permissions.roleRules")}</span>
                        <h2>{t("permissions.chooseRole")}</h2>
                    </div>
                    <span className="feature-count">{t("permissions.count", { count: catalog.length })}</span>
                </div>
                <div className="permission-role-tabs">
                    {roles.map((role) => (
                        <button
                            key={role}
                            type="button"
                            className={"permission-role-tab " + (activeRole === role ? "active" : "")}
                            onClick={() => changeRole(role)}
                        >
                            <span className={"role-badge role-" + role.toLowerCase()}>{translatedEnum(t, "roles.role", role)}</span>
                            <strong>{t("permissions.roleDescription." + role)}</strong>
                        </button>
                    ))}
                </div>
            </section>

            {loading ? <p>{t("permissions.loading")}</p> : (
                <section className="feature-card permissions-list-card">
                    <div className="feature-card-heading">
                        <div>
                            <span className="page-label">{translatedEnum(t, "roles.role", activeRole)} · {t("permissions.title")}</span>
                            <h2>{t("permissions.accessGranted")}</h2>
                        </div>
                        <button type="button" className="product-add-button" onClick={savePermissions} disabled={saving}>
                            {saving ? t("permissions.saving") : t("permissions.save")}
                        </button>
                    </div>

                    {Object.entries(groupedPermissions).map(([category, permissions]) => (
                        <div className="permission-group" key={category}>
                            <div className="permission-group-heading">{categoryText(t, category)}</div>
                            <div className="permission-grid">
                                {permissions.map((permission) => {
                                    const locked = permission.code === "PERMISSIONS_MANAGE" && activeRole !== "ADMIN";
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
                                                {locked && <em>{t("permissions.availableOnlyAdmin")}</em>}
                                            </span>
                                        </label>
                                    );
                                })}
                            </div>
                        </div>
                    ))}

                    <p className="permissions-note">{t("permissions.note")}</p>
                </section>
            )}
        </div>
    );
}
