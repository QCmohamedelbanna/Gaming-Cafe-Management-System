import { useEffect } from "react";

function money(value) {
    return `${Number(value || 0).toFixed(2)} EGP`;
}

export default function CancelBillModal({
    bill,
    cancelling,
    error,
    onClose,
    onConfirm,
}) {
    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !cancelling) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [cancelling, onClose]);

    return (
        <div
            className="modal-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !cancelling) {
                    onClose();
                }
            }}
        >
            <div
                className="modal-container cancel-bill-modal"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="cancel-bill-title"
                aria-describedby="cancel-bill-description"
            >
                <div className="cancel-bill-icon" aria-hidden="true">!</div>

                <div className="cancel-bill-content">
                    <span className="page-label">BILLING ACTION</span>
                    <h2 id="cancel-bill-title">Cancel bill?</h2>
                    <p id="cancel-bill-description">
                        Are you sure you want to cancel <strong>{bill.billNumber}</strong>?
                        This bill will be removed from pending payments.
                    </p>

                    <div className="cancel-bill-summary">
                        <div>
                            <span>Bill</span>
                            <strong>{bill.billNumber}</strong>
                        </div>
                        <div>
                            <span>Reference</span>
                            <strong>
                                {bill.sessionId
                                    ? `Session #${bill.sessionId}`
                                    : `Order #${bill.orderId}`}
                            </strong>
                        </div>
                        <div>
                            <span>Amount</span>
                            <strong>{money(bill.totalAmount)}</strong>
                        </div>
                    </div>

                    <div className="cancel-bill-note">
                        Cancelled bills cannot be paid and will remain recorded in billing history.
                    </div>

                    {error && (
                        <div className="cancel-bill-inline-error" role="alert">
                            <strong>Could not cancel this bill</strong>
                            <span>{error}</span>
                        </div>
                    )}
                </div>

                <div className="cancel-bill-actions">
                    <button
                        type="button"
                        className="product-secondary-button"
                        disabled={cancelling}
                        onClick={onClose}
                    >
                        Keep bill
                    </button>

                    <button
                        type="button"
                        className="cancel-bill-confirm-button"
                        disabled={cancelling}
                        onClick={onConfirm}
                    >
                        {cancelling ? "Cancelling..." : "Cancel bill"}
                    </button>
                </div>
            </div>
        </div>
    );
}
