import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";

function money(value) {
    return `${Number(value || 0).toFixed(2)} EGP`;
}

function isStockLimitReached(item) {
    return item.product?.trackStock === true
        && Number(item.quantity) >= Number(item.product.currentStock ?? 0);
}

export default function OrderCart({
    order,
    attachedSession,
    onUpdateQuantity,
    onRemove,
    onHold,
    onCancel,
    onComplete,
    onApplyDiscount,
    onClearDiscount,
    completing = false,
    discounting = false,
    discountError = "",
}) {
    const { t, formatCurrency } = useLanguage();
    const [discountOpen, setDiscountOpen] = useState(false);
    const [discountType, setDiscountType] = useState("PERCENTAGE");
    const [discountValue, setDiscountValue] = useState("");
    const [discountReason, setDiscountReason] = useState("");

    useEffect(() => {
        setDiscountOpen(false);
        setDiscountValue("");
        setDiscountReason("");
    }, [order?.id]);

    if (!order) {
        return (
            <div className="order-cart">
                <div className="cart-header">
                    <div>
                        <span className="page-label">{t("pos.currentOrder")}</span>
                        <h2>{t("pos.noActiveOrder")}</h2>
                    </div>
                    <span className="order-mode-badge standalone">{t("pos.standby")}</span>
                </div>
                <p className="empty-cart">
                    {t("pos.selectProduct")}
                </p>
            </div>
        );
    }

    const attached = Boolean(attachedSession);
    const subtotal = Number(order.subtotalAmount || order.items?.reduce(
        (sum, item) => sum + Number(item.lineTotal || 0),
        0
    ) || 0);
    const discount = Number(order.discountAmount || 0);
    const total = Number(order.totalAmount || 0);

    function submitDiscount(event) {
        event.preventDefault();
        const value = Number(discountValue);
        if (!Number.isFinite(value) || value < 0 || discounting) return;
        onApplyDiscount({
            type: discountType,
            value,
            reason: discountReason.trim() || null,
        });
    }

    return (
        <div className="order-cart">
            <div className="cart-header">
                <div>
                    <span className="page-label">
                        {attached ? t("cart.sessionOrder") : t("cart.standaloneSale")}
                    </span>
                    <h2>Order #{order.id}</h2>
                    {attached ? (
                        <small className="cart-session-reference">
                            {attachedSession.device?.name} · {attachedSession.sessionType === "SINGLE" ? t("modal.single") : attachedSession.sessionType === "MULTI" ? t("modal.multi") : t("modal.match")} · {t("common.session")} #{attachedSession.id}
                        </small>
                    ) : (
                        <small className="cart-session-reference">{t("cart.cafeOnly")}</small>
                    )}
                </div>
                <span className={`order-mode-badge ${attached ? "attached" : "standalone"}`}>
                    {attached ? t("cart.attached") : t("cart.standalone")}
                </span>
            </div>

            <div className="cart-items">
                {order.items?.length === 0 && (
                    <p className="empty-cart">{t("cart.noProducts")}</p>
                )}

                {order.items?.map((item) => (
                    <div className="cart-item" key={item.id}>
                        <div className="cart-item-info">
                            <strong>{item.product.name}</strong>
                            <span>{formatCurrency(item.unitPriceSnapshot)} {t("cart.each")}</span>
                        </div>

                        <div className="cart-item-right">
                            <div className="quantity-control" aria-label={t("cart.quantity", { name: item.product.name })}>
                                <button
                                    type="button"
                                    aria-label={t("cart.decrease", { name: item.product.name })}
                                    disabled={completing}
                                    onClick={() => onUpdateQuantity(item.id, item.quantity - 1)}
                                >
                                    −
                                </button>
                                <strong>{item.quantity}</strong>
                                <button
                                    type="button"
                                    aria-label={t("cart.increase", { name: item.product.name })}
                                    disabled={completing || isStockLimitReached(item)}
                                    title={isStockLimitReached(item) ? t("cart.stockLimit") : undefined}
                                    onClick={() => onUpdateQuantity(item.id, item.quantity + 1)}
                                >
                                    +
                                </button>
                            </div>
                            <strong>{formatCurrency(item.lineTotal)}</strong>
                            <button
                                type="button"
                                className="remove-item"
                                aria-label={t("cart.remove", { name: item.product.name })}
                                disabled={completing}
                                onClick={() => onRemove(item.id)}
                            >
                                ×
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            <div className="cart-totals">
                <div><span>{t("pos.subtotal")}</span><strong>{formatCurrency(subtotal)}</strong></div>
                {discount > 0 && (
                    <div className="cart-discount-row">
                        <span>{t("pos.discount")}</span>
                        <strong>-{formatCurrency(discount)}</strong>
                    </div>
                )}
                <div className="cart-total">
                    <span>{t("pos.total")}</span>
                    <strong>{formatCurrency(total)}</strong>
                </div>
            </div>

            <div className="cart-discount-actions">
                {discount > 0 && (
                    <button
                        type="button"
                        className="cart-link-button"
                        disabled={discounting || completing}
                        onClick={onClearDiscount}
                    >
                        {t("cart.removeDiscount")}
                    </button>
                )}
                <button
                    type="button"
                    className="cart-link-button"
                    disabled={discounting || completing || !order.items?.length}
                    onClick={() => setDiscountOpen((open) => !open)}
                >
                    {discountOpen ? t("cart.closeDiscount") : t("cart.addDiscount")}
                </button>
            </div>

            {discountOpen && (
                <form className="cart-discount-form" onSubmit={submitDiscount}>
                    <div className="cart-discount-form-grid">
                        <label>
                            {t("cart.discountType")}
                            <select value={discountType} onChange={(event) => setDiscountType(event.target.value)}>
                                <option value="PERCENTAGE">{t("cart.percentage")}</option>
                                <option value="FIXED">{t("cart.fixedAmount")}</option>
                            </select>
                        </label>
                        <label>
                            {t("cart.value")}
                            <input
                                type="number"
                                min="0"
                                max={discountType === "PERCENTAGE" ? "100" : undefined}
                                step="0.01"
                                value={discountValue}
                                onChange={(event) => setDiscountValue(event.target.value)}
                                placeholder={discountType === "PERCENTAGE" ? "10" : "5.00"}
                                autoFocus
                            />
                        </label>
                    </div>
                    <label>
                        {t("cart.reasonOptional")}
                        <input
                            maxLength="200"
                            value={discountReason}
                            onChange={(event) => setDiscountReason(event.target.value)}
                            placeholder={t("cart.managerApproval")}
                        />
                    </label>
                    <p className="permission-hint">{t("cart.permissionHint")}</p>
                    {discountError && <div className="checkout-inline-error" role="alert">{discountError}</div>}
                    <button
                        type="submit"
                        className="primary-action cart-discount-submit"
                        disabled={discounting || discountValue === ""}
                    >
                        {discounting ? t("cart.applying") : t("pos.applyDiscount")}
                    </button>
                </form>
            )}

            {attached ? (
                <div className="session-order-lock-note">
                    {t("cart.sessionLock")}
                </div>
            ) : (
                <div className="cart-action-row">
                    <button
                        type="button"
                        className="cart-secondary-action"
                        disabled={completing || !order.items?.length}
                        onClick={onHold}
                    >
                        {t("cart.holdOrder")}
                    </button>
                    <button
                        type="button"
                        className="cart-cancel-action"
                        disabled={completing}
                        onClick={onCancel}
                    >
                        {t("cart.cancel")}
                    </button>
                </div>
            )}

            <button
                type="button"
                className="checkout-button"
                disabled={
                    attached ||
                    completing ||
                    !order.items ||
                    order.items.length === 0
                }
                onClick={onComplete}
                title={attached ? "Paid during combined session checkout" : undefined}
            >
                {completing
                    ? t("cart.completing")
                    : attached
                        ? t("cart.paidWithSession")
                        : t("cart.completeOrder")}
            </button>
        </div>
    );
}
