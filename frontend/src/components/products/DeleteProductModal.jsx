import { useEffect } from "react";

export default function DeleteProductModal({
    product,
    deleting,
    error,
    onClose,
    onConfirm,
}) {
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
                    <span className="page-label">PERMANENT ACTION</span>
                    <h2 id="delete-product-title">Delete product?</h2>
                    <p id="delete-product-description">
                        <strong>{product.name}</strong> will be permanently removed.
                        This action cannot be undone.
                    </p>

                    <div className="delete-product-summary">
                        <span>{product.name}</span>
                        <strong>{Number(product.sellingPrice ?? product.price ?? 0).toFixed(2)} EGP</strong>
                    </div>

                    <div className="delete-product-note">
                        A product in an open order cannot be deleted until checkout
                        is complete. Completed sales will keep their historical item
                        details after this product is removed.
                    </div>

                    {error && (
                        <div className="delete-product-inline-error" role="alert">
                            <strong>Could not delete this product</strong>
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
                        Cancel
                    </button>

                    <button
                        type="button"
                        className="confirm-delete-product-button"
                        disabled={deleting}
                        onClick={onConfirm}
                    >
                        {deleting ? "Deleting..." : "Delete"}
                    </button>
                </div>
            </div>
        </div>
    );
}
