import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

export default function ProductFormModal({ product, saving, onClose, onSave }) {
    const { t } = useLanguage();
    const [name, setName] = useState(product?.name ?? "");
    const [sku, setSku] = useState(product?.sku ?? "");
    const [category, setCategory] = useState(product?.category ?? "");
    const [sellingPrice, setSellingPrice] = useState(
        product?.sellingPrice ?? product?.price ?? ""
    );
    const [costPrice, setCostPrice] = useState(product?.costPrice ?? "");
    const [trackStock, setTrackStock] = useState(
        product ? Boolean(product.trackStock) : true
    );
    const [minimumStock, setMinimumStock] = useState(product?.minimumStock ?? "");
    const [unit, setUnit] = useState(product?.unit ?? "unit");
    const editing = Boolean(product);
    const valid = name.trim() && Number(sellingPrice) > 0;

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !saving) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onClose, saving]);

    function submit(event) {
        event.preventDefault();
        if (!valid || saving) return;
        onSave({
            name: name.trim(),
            price: Number(sellingPrice),
            sellingPrice: Number(sellingPrice),
            sku: sku.trim() || null,
            category: category.trim() || "Uncategorized",
            costPrice: costPrice === "" ? 0 : Number(costPrice),
            trackStock,
            minimumStock: minimumStock === "" ? 0 : Number(minimumStock),
            unit: unit.trim() || "unit",
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
                className="modal-container product-form-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="product-form-title"
                onSubmit={submit}
            >
                <div className="modal-header">
                    <div>
                        <span className="page-label">{t("nav.products")}</span>
                        <h2 id="product-form-title">
                            {editing ? `${t("common.edit")} ${t("products.title")}` : t("products.addProduct")}
                        </h2>
                        <p>
                            {editing
                                ? t("products.updateDescription")
                                : t("products.createDescription")}
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

                <label htmlFor="product-name">{t("products.productName")}</label>
                <input
                    id="product-name"
                    autoFocus
                    maxLength="100"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder={t("products.exampleName")}
                />

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="product-sku">{t("form.sku")}</label>
                        <input
                            id="product-sku"
                            maxLength="80"
                            value={sku}
                            onChange={(event) => setSku(event.target.value)}
                            placeholder={t("products.optionalBarcode")}
                        />
                    </div>
                    <div>
                        <label htmlFor="product-category">{t("form.category")}</label>
                        <input
                            id="product-category"
                            maxLength="80"
                            value={category}
                            onChange={(event) => setCategory(event.target.value)}
                            placeholder={t("products.categoryPlaceholder")}
                        />
                    </div>
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="product-selling-price">{t("form.sellingPrice")}</label>
                        <div className="product-price-input">
                            <input
                                id="product-selling-price"
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={sellingPrice}
                                onChange={(event) => setSellingPrice(event.target.value)}
                                placeholder="0.00"
                            />
                            <span>{t("common.egp")}</span>
                        </div>
                    </div>
                    <div>
                        <label htmlFor="product-cost-price">{t("form.costPrice")}</label>
                        <div className="product-price-input">
                            <input
                                id="product-cost-price"
                                type="number"
                                min="0"
                                step="0.01"
                                value={costPrice}
                                onChange={(event) => setCostPrice(event.target.value)}
                                placeholder="0.00"
                            />
                            <span>{t("common.egp")}</span>
                        </div>
                    </div>
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="product-minimum-stock">{t("form.minimumStock")}</label>
                        <input
                            id="product-minimum-stock"
                            type="number"
                            min="0"
                            step="0.001"
                            value={minimumStock}
                            onChange={(event) => setMinimumStock(event.target.value)}
                            placeholder="0"
                        />
                    </div>
                    <div>
                        <label htmlFor="product-unit">{t("form.unit")}</label>
                        <input
                            id="product-unit"
                            maxLength="30"
                            value={unit}
                            onChange={(event) => setUnit(event.target.value)}
                            placeholder={t("products.piecePlaceholder")}
                        />
                    </div>
                </div>

                <label className="product-track-toggle">
                    <input
                        type="checkbox"
                        checked={trackStock}
                        onChange={(event) => setTrackStock(event.target.checked)}
                    />
                    <span>
                        <strong>{t("products.trackStockLabel")}</strong>
                        <small>{t("products.trackStockHelp")}</small>
                    </span>
                </label>

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
                        disabled={!valid || saving}
                    >
                        {saving
                            ? t("common.working")
                            : editing ? t("common.save") : t("products.addProduct")}
                    </button>
                </div>
            </form>
        </div>
    );
}
