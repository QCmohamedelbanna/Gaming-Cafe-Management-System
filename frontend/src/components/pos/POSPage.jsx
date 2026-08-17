import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../../i18n";
import { useAuth } from "../../auth/AuthContext";

import { getProducts } from "../../api/productApi";
import {
    addOrderItem,
    applyOrderDiscount,
    cancelOrder,
    clearOrderDiscount,
    completeOrder,
    createOrder,
    getHeldOrders,
    getOpenOrderForSession,
    holdOrder,
    removeOrderItem,
    resumeOrder,
    updateOrderItemQuantity,
} from "../../api/orderApi";
import { refundBill } from "../../api/billingApi";

import ProductCard from "./ProductCard";
import OrderCart from "./OrderCart";
import POSPaymentModal from "./POSPaymentModal";
import CancelOrderModal from "./CancelOrderModal";
import CategoryDropdown from "./CategoryDropdown";
import ReceiptModal from "../dashboard/ReceiptModal";

export default function POSPage({
    attachedSession = null,
    onStartStandalone = null,
}) {
    const { t, formatCurrency, formatNumber, language } = useLanguage();
    const { hasPermission } = useAuth();
    const canRefund = hasPermission("BILL_REFUND");
    const [products, setProducts] = useState([]);
    const [order, setOrder] = useState(null);
    const [heldOrders, setHeldOrders] = useState([]);

    const [loading, setLoading] = useState(true);
    const [addingProductId, setAddingProductId] = useState(null);
    const [updatingItemId, setUpdatingItemId] = useState(null);
    const [completing, setCompleting] = useState(false);
    const [discounting, setDiscounting] = useState(false);

    const [search, setSearch] = useState("");
    const [category, setCategory] = useState("");
    const [barcode, setBarcode] = useState("");
    const [paymentOpen, setPaymentOpen] = useState(false);
    const [paymentError, setPaymentError] = useState("");
    const [discountError, setDiscountError] = useState("");
    const [cancelOpen, setCancelOpen] = useState(false);
    const [cancelling, setCancelling] = useState(false);
    const [cancelError, setCancelError] = useState("");
    const [message, setMessage] = useState("");
    const [receipt, setReceipt] = useState(null);
    const [refunding, setRefunding] = useState(false);

    const categories = useMemo(() => Array.from(new Set(
        products
            .map((product) => product.category)
            .filter(Boolean)
    )).sort((left, right) => left.localeCompare(right)), [products]);

    const visibleProducts = useMemo(() => {
        const normalizedSearch = search.trim().toLowerCase();
        return products.filter((product) => {
            const matchesSearch = !normalizedSearch
                || product.name?.toLowerCase().includes(normalizedSearch)
                || product.sku?.toLowerCase().includes(normalizedSearch)
                || product.category?.toLowerCase().includes(normalizedSearch);
            const matchesCategory = !category || product.category === category;
            return matchesSearch && matchesCategory;
        });
    }, [products, search, category]);

    useEffect(() => {
        loadPOS();
    }, [attachedSession?.id]);

    async function loadPOS() {
        try {
            setLoading(true);
            setMessage("");
            const [productData, sessionOrder, held] = await Promise.all([
                getProducts(),
                attachedSession
                    ? getOpenOrderForSession(attachedSession.id)
                    : Promise.resolve(null),
                getHeldOrders(),
            ]);
            setProducts(productData || []);
            setOrder(sessionOrder || null);
            setHeldOrders(held || []);
        } catch (error) {
            console.error(error);
            setMessage(error.message || t("pos.loadError"));
        } finally {
            setLoading(false);
        }
    }

    async function refreshHeldOrders() {
        try {
            setHeldOrders(await getHeldOrders());
        } catch (error) {
            console.error(error);
            setMessage(error.message || t("pos.heldLoadError"));
        }
    }

    async function handleAddProduct(product) {
        const quantityInOrder = Number(
            order?.items?.find((item) => item.product?.id === product.id)?.quantity || 0
        );
        const availableStock = product.trackStock === true
            ? Math.max(0, Number(product.currentStock ?? 0))
            : null;

        if (availableStock !== null && quantityInOrder + 1 > availableStock) {
            setMessage(t("pos.notEnoughStock", { name: product.name }));
            return;
        }

        try {
            setAddingProductId(product.id);
            setMessage("");

            let currentOrder = order;
            if (!currentOrder) {
                currentOrder = await createOrder(attachedSession?.id ?? null);
            }

            setOrder(await addOrderItem(currentOrder.id, product.id, 1));
        } catch (error) {
            console.error(error);
            setMessage(error.message || t("pos.addError"));
        } finally {
            setAddingProductId(null);
        }
    }

    async function handleBarcodeSubmit(event) {
        event.preventDefault();
        const value = barcode.trim().toLowerCase();
        if (!value) return;

        const product = products.find((candidate) =>
            candidate.sku?.trim().toLowerCase() === value
        );

        if (!product) {
            setMessage(t("pos.noBarcodeProduct", { barcode: barcode.trim() }));
            return;
        }

        await handleAddProduct(product);
        setBarcode("");
    }

    async function handleUpdateQuantity(itemId, quantity) {
        if (!order) return;
        try {
            setUpdatingItemId(itemId);
            setMessage("");
            const updated = quantity <= 0
                ? await removeOrderItem(order.id, itemId)
                : await updateOrderItemQuantity(order.id, itemId, quantity);
            setOrder(updated?.status === "OPEN" ? updated : null);
        } catch (error) {
            console.error(error);
            setMessage(error.message || t("pos.updateQuantityError"));
        } finally {
            setUpdatingItemId(null);
        }
    }

    async function handleRemove(itemId) {
        if (!order) return;
        await handleUpdateQuantity(itemId, 0);
    }

    async function handleHold() {
        if (!order) return;
        try {
            setCompleting(true);
            setMessage("");
            await holdOrder(order.id);
            setOrder(null);
            await refreshHeldOrders();
            setMessage(t("pos.orderHeldMessage"));
        } catch (error) {
            console.error(error);
            setMessage(error.message || t("pos.holdError"));
        } finally {
            setCompleting(false);
        }
    }

    function handleRequestCancel() {
        if (!order) return;
        setCancelError("");
        setCancelOpen(true);
    }

    function handleCloseCancel() {
        if (!cancelling) setCancelOpen(false);
    }

    async function handleCancel() {
        if (!order) return;

        try {
            setCancelling(true);
            setCancelError("");
            setMessage("");
            await cancelOrder(order.id);
            setCancelOpen(false);
            setOrder(null);
            await refreshHeldOrders();
            setMessage(t("pos.orderCancelledMessage", { id: order.id }));
        } catch (error) {
            console.error(error);
            setCancelError(error.message || t("pos.cancelError"));
        } finally {
            setCancelling(false);
        }
    }

    async function handleResume(heldOrder) {
        if (order?.items?.length) {
            setMessage(t("pos.currentOrderRequired"));
            return;
        }

        try {
            setCompleting(true);
            setMessage("");
            setOrder(await resumeOrder(heldOrder.id));
            await refreshHeldOrders();
            setMessage(t("pos.orderResumedMessage", { id: heldOrder.id }));
        } catch (error) {
            console.error(error);
            setMessage(error.message || t("pos.resumeError"));
        } finally {
            setCompleting(false);
        }
    }

    async function handleApplyDiscount(data) {
        if (!order) return;
        try {
            setDiscounting(true);
            setDiscountError("");
            setOrder(await applyOrderDiscount(order.id, data));
            setMessage(t("pos.discountApplied"));
        } catch (error) {
            console.error(error);
            setDiscountError(error.message || t("pos.discountError"));
        } finally {
            setDiscounting(false);
        }
    }

    async function handleClearDiscount() {
        if (!order) return;
        try {
            setDiscounting(true);
            setDiscountError("");
            setOrder(await clearOrderDiscount(order.id));
            setMessage(t("pos.discountRemoved"));
        } catch (error) {
            console.error(error);
            setDiscountError(error.message || t("pos.removeDiscountError"));
        } finally {
            setDiscounting(false);
        }
    }

    function handleOpenPayment() {
        if (!order || attachedSession) return;
        setPaymentError("");
        setPaymentOpen(true);
    }

    async function handleComplete(payment) {
        if (!order) return;

        try {
            setCompleting(true);
            setPaymentError("");
            const completed = await completeOrder(order.id, payment);
            setPaymentOpen(false);
            setMessage(t("pos.completedMessage", { bill: completed.billNumber, total: formatCurrency(completed.totalAmount) }));
            setReceipt(completed);
            setOrder(null);
        } catch (error) {
            console.error(error);
            setPaymentError(error.message || t("pos.completeError"));
        } finally {
            setCompleting(false);
        }
    }

    async function handleRefund(reason) {
        if (!receipt?.billId) return;

        try {
            setRefunding(true);
            setReceipt(await refundBill(receipt.billId, reason));
        } catch (error) {
            console.error(error);
            setMessage(error.message || t("billing.refundError"));
        } finally {
            setRefunding(false);
        }
    }

    if (loading) return <p>{t("common.loading")}</p>;

    return (
        <div className="pos-page">
            <div className="pos-header">
                <div>
                    <span className="page-label">{t("pos.pageLabel")}</span>
                    <h2>{attachedSession ? t("pos.sessionOrder") : t("pos.standaloneCafeSale")}</h2>
                    <p>
                        {attachedSession
                            ? t("pos.sessionDescription")
                            : t("pos.standaloneDescription")}
                    </p>
                </div>

                <div className="pos-header-actions">
                    <span className={`pos-mode-badge ${attachedSession ? "attached" : "standalone"}`}>
                        {attachedSession ? t("pos.sessionAttached") : t("pos.standaloneSale")}
                    </span>
                    {attachedSession && onStartStandalone && (
                        <button type="button" className="product-secondary-button" onClick={onStartStandalone}>
                            {t("pos.newStandaloneSale")}
                        </button>
                    )}
                </div>
            </div>

            {attachedSession && (
                <div className="attached-session-card pos-attached-banner">
                    <span className="attached-label">{t("pos.attachedTo")}</span>
                    <strong>{attachedSession.device?.name}</strong>
                    <small>{t("pos.sessionReference", { id: attachedSession.id, type: attachedSession.sessionType === "SINGLE" ? t("modal.single") : attachedSession.sessionType === "MULTI" ? t("modal.multi") : t("modal.match") })}</small>
                </div>
            )}

            {message && <div className="pricing-message">{message}</div>}

            {!attachedSession && heldOrders.length > 0 && (
                <section className="held-orders-panel">
                    <div className="held-orders-header">
                        <div>
                            <span className="page-label">{t("pos.heldOrdersLabel")}</span>
                            <strong>{t("pos.ordersWaiting", { count: formatNumber(heldOrders.length), suffix: language === "ar" ? "" : heldOrders.length === 1 ? "" : "s" })}</strong>
                        </div>
                        <small>{t("pos.resumeCafeSale")}</small>
                    </div>
                    <div className="held-orders-list">
                        {heldOrders.map((held) => (
                            <div className="held-order-row" key={held.id}>
                                <div>
                                    <strong>{t("pos.orderReference", { id: held.id })}</strong>
                                    <span>{t("pos.itemLines", { count: formatNumber(held.items?.length || 0), suffix: language === "ar" ? "" : held.items?.length === 1 ? "" : "s" })} · {formatCurrency(held.totalAmount || 0)}</span>
                                </div>
                                <button type="button" className="product-secondary-button" disabled={completing} onClick={() => handleResume(held)}>
                                    {t("pos.heldAction")}
                                </button>
                            </div>
                        ))}
                    </div>
                </section>
            )}

            <div className="pos-toolbar">
                <label className="pos-search-field">
                    <span>{t("pos.searchShort")}</span>
                    <input
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder={t("pos.searchPlaceholder")}
                    />
                </label>
                <div className="pos-category-field">
                    <span>{t("pos.categories")}</span>
                    <CategoryDropdown
                        categories={categories}
                        value={category}
                        onChange={setCategory}
                    />
                </div>
                <form className="pos-barcode-form" onSubmit={handleBarcodeSubmit}>
                    <label htmlFor="pos-barcode">{t("pos.barcodeSku")}</label>
                    <div>
                        <input
                            id="pos-barcode"
                            value={barcode}
                            onChange={(event) => setBarcode(event.target.value)}
                            placeholder={t("pos.scanEnter")}
                        />
                        <button type="submit" disabled={!barcode.trim() || addingProductId != null}>{t("pos.add")}</button>
                    </div>
                </form>
            </div>

            <div className="pos-layout">
                <section className="products-panel">
                    {visibleProducts.length === 0 ? (
                        <div className="products-empty-state">
                            <h2>{t("pos.noMatching")}</h2>
                            <p>{t("pos.tryDifferent")}</p>
                        </div>
                    ) : (
                        <div className="product-grid">
                            {visibleProducts.map((product) => (
                                <ProductCard
                                    key={product.id}
                                    product={product}
                                    disabled={addingProductId === product.id}
                                    quantityInOrder={Number(
                                        order?.items?.find((item) => item.product?.id === product.id)?.quantity || 0
                                    )}
                                    onAdd={handleAddProduct}
                                />
                            ))}
                        </div>
                    )}
                </section>

                <OrderCart
                    order={order}
                    attachedSession={attachedSession}
                    onUpdateQuantity={handleUpdateQuantity}
                    onRemove={handleRemove}
                    onHold={handleHold}
                    onCancel={handleRequestCancel}
                    onComplete={handleOpenPayment}
                    onApplyDiscount={handleApplyDiscount}
                    onClearDiscount={handleClearDiscount}
                    completing={completing || cancelling || updatingItemId != null}
                    discounting={discounting}
                    discountError={discountError}
                />
            </div>

            {paymentOpen && order && (
                <POSPaymentModal
                    order={order}
                    loading={completing}
                    error={paymentError}
                    onClose={() => setPaymentOpen(false)}
                    onConfirm={handleComplete}
                />
            )}

            {cancelOpen && order && (
                <CancelOrderModal
                    order={order}
                    cancelling={cancelling}
                    error={cancelError}
                    onClose={handleCloseCancel}
                    onConfirm={handleCancel}
                />
            )}

            {receipt && (
                <ReceiptModal
                    bill={receipt}
                    refunding={refunding}
                    canRefund={canRefund}
                    onClose={() => setReceipt(null)}
                    onRefund={handleRefund}
                />
            )}
        </div>
    );
}
