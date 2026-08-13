import { useEffect, useState } from "react";

export default function StockMovementModal({
    product,
    mode,
    saving,
    onClose,
    onSave,
}) {
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
                        <span className="page-label">INVENTORY LEDGER</span>
                        <h2 id="stock-movement-title">
                            {purchase ? "Record purchase" : waste ? "Record waste" : "Adjust stock"}
                        </h2>
                        <p>
                            {product?.name} · Current stock {Number(product?.currentStock ?? 0).toFixed(3)} {product?.unit || "unit"}
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

                <div className="stock-entry-callout">
                    {purchase
                        ? "Purchases increase the cached balance and create a PURCHASE ledger entry."
                        : waste
                            ? "Waste removes stock and creates a WASTE ledger entry. Negative balances are blocked when configured."
                            : "Enter a positive number to add stock or a negative number to remove it."}
                </div>

                <div className="product-form-grid">
                    <div>
                        <label htmlFor="stock-quantity">
                            {purchase ? "Quantity purchased" : waste ? "Quantity wasted" : "Quantity change"}
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
                        <label htmlFor="stock-unit-cost">Unit cost</label>
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
                            <span>EGP</span>
                        </div>
                    </div>
                </div>

                <label htmlFor="stock-reference">Reference</label>
                <input
                    id="stock-reference"
                    maxLength="160"
                    value={reference}
                    onChange={(event) => setReference(event.target.value)}
                    placeholder={purchase ? "Supplier invoice or delivery note" : "Reason or count sheet"}
                />

                <label htmlFor="stock-created-by">Recorded by</label>
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
                        Cancel
                    </button>
                    <button
                        type="submit"
                        className="primary-action"
                        disabled={!valid}
                    >
                        {saving ? "Saving..." : purchase ? "Record purchase" : waste ? "Record waste" : "Save adjustment"}
                    </button>
                </div>
            </form>
        </div>
    );
}
