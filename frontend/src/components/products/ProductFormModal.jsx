import { useEffect, useState } from "react";

export default function ProductFormModal({ product, saving, onClose, onSave }) {
    const [name, setName] = useState(product?.name ?? "");
    const [price, setPrice] = useState(product?.price ?? "");
    const editing = Boolean(product);
    const valid = name.trim() && Number(price) > 0;

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
        onSave({ name: name.trim(), price: Number(price) });
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
                                ? "Update the product name or price."
                                : "Create a product for POS and quick orders."}
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

                <label htmlFor="product-price">Price</label>
                <div className="product-price-input">
                    <input
                        id="product-price"
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={price}
                        onChange={(event) => setPrice(event.target.value)}
                        placeholder="0.00"
                    />
                    <span>EGP</span>
                </div>

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
