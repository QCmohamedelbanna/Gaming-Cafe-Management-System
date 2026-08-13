import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

export default function ReceiptModal({
    bill,
    refunding = false,
    onClose,
    onRefund = null,
}) {
    const { t, formatCurrency } = useLanguage();
    const formatMoney = formatCurrency;
    const [refundOpen, setRefundOpen] = useState(false);
    const [reason, setReason] = useState("");
    const refundable = bill.status === "PAID" && Boolean(onRefund);

    useEffect(() => {
        function handleKeyDown(event) {
            if (event.key === "Escape" && !refunding) onClose();
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [onClose, refunding]);

    function submitRefund(event) {
        event.preventDefault();
        if (!reason.trim() || refunding) return;
        onRefund(reason.trim());
    }

    return (
        <div
            className="modal-overlay receipt-overlay"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !refunding) onClose();
            }}
        >
            <div
                className="modal-container receipt-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="receipt-title"
            >
                <div className="modal-header receipt-header no-print">
                    <div>
                        <span className="page-label">{t("modal.paymentComplete")}</span>
                        <h2 id="receipt-title">{t("modal.receipt")}</h2>
                        <p>{bill.billNumber}</p>
                    </div>
                    <button
                        type="button"
                        className="modal-close"
                        disabled={refunding}
                        onClick={onClose}
                        aria-label={t("modal.closeReceipt")}
                    >
                        &times;
                    </button>
                </div>

                <div className="receipt-print-area">
                    <div className="receipt-brand">{t("brand.name")}</div>
                    <div className="receipt-number">{bill.billNumber}</div>
                    <div className="receipt-sale-type">
                        {bill.sessionId ? t("modal.sessionCheckout") : t("modal.standaloneCafeSale")}
                    </div>

                    {bill.lines?.length > 0 && (
                        <div className="receipt-lines">
                            {bill.lines.map((line) => (
                                <div className="receipt-line" key={line.productId}>
                                    <span>
                                        {line.productName} × {line.quantity}
                                    </span>
                                    <strong>{formatMoney(line.lineTotal)}</strong>
                                </div>
                            ))}
                        </div>
                    )}

                    <div className="receipt-amounts">
                        <div><span>{t("modal.gaming")}</span><strong>{formatMoney(bill.gamingAmount)}</strong></div>
                        {Number(bill.orderSubtotal || 0) > 0 && (
                            <div><span>{t("modal.productsSubtotal")}</span><strong>{formatMoney(bill.orderSubtotal)}</strong></div>
                        )}
                        {Number(bill.discountAmount || 0) > 0 && (
                            <div className="receipt-discount-row"><span>{t("modal.discountAmount")}</span><strong>-{formatMoney(bill.discountAmount)}</strong></div>
                        )}
                        <div><span>{t("modal.products")}</span><strong>{formatMoney(bill.orderAmount)}</strong></div>
                        <div className="receipt-grand-total"><span>{t("modal.total")}</span><strong>{formatMoney(bill.totalAmount)}</strong></div>
                    </div>

                    {bill.paymentMethod && (
                        <div className="receipt-payment-details">
                            <div><span>{t("modal.payment")}</span><strong>{bill.paymentMethod === "CASH" ? t("common.cash") : bill.paymentMethod === "CARD" ? t("common.card") : t("common.mobileWallet")}</strong></div>
                            <div><span>{t("modal.received")}</span><strong>{formatMoney(bill.amountTendered)}</strong></div>
                            <div><span>{t("modal.change")}</span><strong>{formatMoney(bill.changeAmount)}</strong></div>
                        </div>
                    )}

                    <div className={`receipt-status ${String(bill.status).toLowerCase()}`}>
                        {bill.status === "PAID" ? t("common.paid") : bill.status === "CANCELLED" ? t("common.cancelled") : bill.status === "REFUNDED" ? t("common.refunded") : t("common.pending")}
                    </div>
                </div>

                {refundOpen && refundable && (
                    <form className="refund-form no-print" onSubmit={submitRefund}>
                        <label htmlFor="refund-reason">{t("modal.refundReason")}</label>
                        <textarea
                            id="refund-reason"
                            rows="3"
                            value={reason}
                            onChange={(event) => setReason(event.target.value)}
                            placeholder={t("modal.refundPlaceholder")}
                            autoFocus
                        />
                        <div className="refund-form-actions">
                            <button
                                type="button"
                                className="product-secondary-button"
                                disabled={refunding}
                                onClick={() => setRefundOpen(false)}
                            >
                                {t("modal.keepBill")}
                            </button>
                            <button
                                type="submit"
                                className="confirm-delete-product-button"
                                disabled={refunding || !reason.trim()}
                            >
                                {refunding ? t("modal.refunding") : t("modal.confirmRefund")}
                            </button>
                        </div>
                    </form>
                )}

                <div className="receipt-actions no-print">
                    <button type="button" className="product-secondary-button" onClick={() => window.print()}>
                        {t("common.printReceipt")}
                    </button>
                    {refundable && !refundOpen && (
                        <button
                            type="button"
                            className="receipt-refund-button"
                            disabled={refunding}
                            onClick={() => setRefundOpen(true)}
                        >
                            {t("common.refundBill")}
                        </button>
                    )}
                    <button type="button" className="primary-action" onClick={onClose}>
                        {t("modal.done")}
                    </button>
                </div>
            </div>
        </div>
    );
}
