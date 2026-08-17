import { useEffect } from "react";
import { useLanguage } from "../../i18n";

export default function DeleteRoleModal({
    rule,
    name,
    deleting,
    error,
    onClose,
    onConfirm,
}) {
    const { t } = useLanguage();
    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !deleting) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [deleting, onClose]);

    return (
        <div
            className="modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !deleting) onClose();
            }}
        >
            <div
                className="modal-container delete-role-modal"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="delete-role-title"
                aria-describedby="delete-role-description"
            >
                <div className="delete-product-icon" aria-hidden="true">!</div>

                <div className="delete-product-content">
                    <span className="page-label">{t("roles.deleteLabel")}</span>
                    <h2 id="delete-role-title">{t("roles.deleteTitle")}</h2>
                    <p id="delete-role-description">
                        {t("roles.deleteDescription", { name })}
                    </p>

                    <div className="delete-product-summary">
                        <span>{name}</span>
                        <strong>{t("roles.deleteUsersAssigned", { count: rule.userCount })}</strong>
                    </div>

                    <div className="delete-product-note">
                        {t("roles.deleteNote")}
                    </div>

                    {error && (
                        <div className="delete-product-inline-error" role="alert">
                            <strong>{t("roles.deleteErrorTitle")}</strong>
                            <span>{error}</span>
                        </div>
                    )}
                </div>

                <div className="delete-product-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={deleting}
                        onClick={onClose}
                    >
                        {t("common.cancel")}
                    </button>

                    <button
                        type="button"
                        className="confirm-delete-product-button"
                        disabled={deleting}
                        onClick={onConfirm}
                    >
                        {deleting ? t("roles.deleting") : t("common.delete")}
                    </button>
                </div>
            </div>
        </div>
    );
}
