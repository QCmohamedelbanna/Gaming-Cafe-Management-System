import { useEffect, useState } from "react";

export default function ProductFormModal({ product, saving, onClose, onSave }) {
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
                        <span className="page-label">PRODUCTS</span>
                        <h2 id="product-form-title">
                            {editing ? "Edit Product" : "Add Product"}
                        </h2>
                        <p>
                            {editing
                                ? "Update pricing and inventory settings."
                                : "Create a product for POS and inventory tracking."}
                        </p>
                    </div>
                    <button
                        type="button"
                        className="modal-close"
                        aria-label="Close"
                        disabled={saving}
                        onClick={onClose}
                    >
                        &times;
                    </button>
                </div>

                <label htmlFor="product-name">Product name</label>
                <input
                    id="product-name"
                    autoFocus
                    maxLength="100"
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder="Example: Orange Juice"
                />

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="product-sku">SKU / barcode</label>
                        <input
                            id="product-sku"
                            maxLength="80"
                            value={sku}
                            onChange={(event) => setSku(event.target.value)}
                            placeholder="Optional barcode"
                        />
                    </div>
                    <div>
                        <label htmlFor="product-category">Category</label>
                        <input
                            id="product-category"
                            maxLength="80"
                            value={category}
                            onChange={(event) => setCategory(event.target.value)}
                            placeholder="Drinks, snacks..."
                        />
                    </div>
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="product-selling-price">Selling price</label>
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
                            <span>EGP</span>
                        </div>
                    </div>
                    <div>
                        <label htmlFor="product-cost-price">Cost price</label>
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
                            <span>EGP</span>
                        </div>
                    </div>
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="product-minimum-stock">Minimum stock</label>
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
                        <label htmlFor="product-unit">Unit</label>
                        <input
                            id="product-unit"
                            maxLength="30"
                            value={unit}
                            onChange={(event) => setUnit(event.target.value)}
                            placeholder="piece, bottle..."
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
                        <strong>Track stock for this product</strong>
                        <small>Sales will reduce inventory and refunds will restore it.</small>
                    </span>
                </label>

                <div className="product-form-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={saving}
                        onClick={onClose}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        className="primary-action"
                        disabled={!valid || saving}
                    >
                        {saving
                            ? "Saving..."
                            : editing ? "Save Changes" : "Add Product"}
                    </button>
                </div>
            </form>
        </div>
    );
}
