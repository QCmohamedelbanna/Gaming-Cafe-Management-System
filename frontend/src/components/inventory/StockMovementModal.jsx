import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

export default function StockMovementModal({
    product,
    mode,
    saving,
    onClose,
    onSave,
}) {
    const { t, formatNumber } = useLanguage();
    const purchase = mode === "purchase";
    const waste = mode === "waste";
    const [quantity, setQuantity] = useState("");
    const [unitCost, setUnitCost] = useState(product?.costPrice ?? "");
    const [reference, setReference] = useState("");
    const [createdBy, setCreatedBy] = useState("admin");

    const quantityNumber = Number(quantity);
    const validQuantity = purchase || waste
        ? quantityNumber > 0
        : quantityNumber !== 0;
    const valid = validQuantity
        && (unitCost === "" || Number(unitCost) >= 0)
        && !saving;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !saving) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onClose, saving]);

    function submit(event) {
        event.preventDefault();
        if (!valid) return;

        onSave({
            quantity: quantityNumber,
            unitCost: unitCost === "" ? null : Number(unitCost),
            reference: reference.trim() || null,
            createdBy: createdBy.trim() || "admin",
        });
    }

    return (
        <div
            className="modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !saving) onClose();
            }}
        >
            <form
                className="modal-container stock-movement-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="stock-movement-title"
                onSubmit={submit}
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">{t("inventory.ledger")}</span>
                        <h2 id="stock-movement-title">
                            {purchase ? t("inventory.purchaseTitle") : waste ? t("inventory.wasteTitle") : t("inventory.adjustTitle")}
                        </h2>
                        <p>
                            {product?.name} · {t("inventory.stock")} {formatNumber(product?.currentStock ?? 0, { maximumFractionDigits: 3 })} {product?.unit || t("form.unit")}
                        </p>
                    </div>
                    <button
                        type="button"
                        className="modal-close"
                        aria-label={t("common.close")}
                        disabled={saving}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                <div className="stock-entry-callout">
                    {purchase
                        ? t("inventory.purchaseCallout")
                        : waste
                            ? t("inventory.wasteCallout")
                            : t("inventory.adjustCallout")}
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="stock-quantity">
                            {purchase ? t("inventory.quantityPurchased") : waste ? t("inventory.quantityWasted") : t("inventory.quantityChange")}
                        </label>
                        <input
                            id="stock-quantity"
                            autoFocus
                            type="number"
                            step="0.001"
                            value={quantity}
                            onChange={(event) => setQuantity(event.target.value)}
                            placeholder={purchase || waste ? "0" : "+ / - 0"}
                        />
                    </div>
                    <div>
                        <label htmlFor="stock-unit-cost">{t("inventory.unitCost")}</label>
                        <div className="product-price-input">
                            <input
                                id="stock-unit-cost"
                                type="number"
                                min="0"
                                step="0.01"
                                value={unitCost}
                                onChange={(event) => setUnitCost(event.target.value)}
                                placeholder="0.00"
                            />
                            <span>{t("common.egp")}</span>
                        </div>
                    </div>
                </div>

                <label htmlFor="stock-reference">{t("form.reason")}</label>
                <input
                    id="stock-reference"
                    maxLength="160"
                    value={reference}
                    onChange={(event) => setReference(event.target.value)}
                    placeholder={purchase ? t("inventory.referencePlaceholderPurchase") : t("inventory.referencePlaceholderOther")}
                />

                <label htmlFor="stock-created-by">{t("inventory.recordedBy")}</label>
                <input
                    id="stock-created-by"
                    maxLength="80"
                    value={createdBy}
                    onChange={(event) => setCreatedBy(event.target.value)}
                    placeholder="admin"
                />

                <div className="product-form-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={saving}
                        onClick={onClose}
                    >
                        {t("common.cancel")}
                    </button>
                    <button
                        type="submit"
                        className="primary-action"
                        disabled={!valid}
                    >
                        {saving ? t("common.working") : purchase ? t("inventory.purchaseTitle") : waste ? t("inventory.wasteTitle") : t("inventory.saveAdjustment")}
                    </button>
                </div>
            </form>
        </div>
    );
}
