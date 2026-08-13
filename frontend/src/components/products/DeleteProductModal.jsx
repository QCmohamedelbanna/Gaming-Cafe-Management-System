import { useEffect } from "react";
import { useLanguage } from "../../i18n";

export default function DeleteProductModal({
    product,
    deleting,
    error,
    onClose,
    onConfirm,
}) {
    const { t, formatCurrency } = useLanguage();
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
                className="modal-container delete-product-modal"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="delete-product-title"
                aria-describedby="delete-product-description"
            >
                <div className="delete-product-icon" aria-hidden="true">!</div>

                <div className="delete-product-content">
                    <span className="page-label">{t("products.deleteLabel")}</span>
                    <h2 id="delete-product-title">{t("products.deleteTitle")}</h2>
                    <p id="delete-product-description">
                        {t("products.deleteDescription", { name: product.name })}
                    </p>

                    <div className="delete-product-summary">
                        <span>{product.name}</span>
                        <strong>{formatCurrency(product.sellingPrice ?? product.price ?? 0)}</strong>
                    </div>

                    <div className="delete-product-note">
                        {t("products.deleteNote")}
                    </div>

                    {error && (
                        <div className="delete-product-inline-error" role="alert">
                            <strong>{t("products.deleteErrorTitle")}</strong>
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
                        {deleting ? t("products.deleting") : t("common.delete")}
                    </button>
                </div>
            </div>
        </div>
    );
}
