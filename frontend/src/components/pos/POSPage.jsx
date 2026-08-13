import {useEffect, useState} from "react";

import {getProducts} from "../../api/productApi";

import {
    createOrder, addOrderItem, removeOrderItem, completeOrder,
} from "../../api/orderApi";
import { refundBill } from "../../api/billingApi";

import ProductCard from "./ProductCard";
import OrderCart from "./OrderCart";
import ReceiptModal from "../dashboard/ReceiptModal";

export default function POSPage({
                                    attachedSession = null,
                                }) {
    const [products, setProducts] = useState([]);
    const [order, setOrder] = useState(null);

    const [loading, setLoading] = useState(true);
    const [addingProductId, setAddingProductId] = useState(null);

    const [completing, setCompleting] = useState(false);

    const [message, setMessage] = useState("");
    const [receipt, setReceipt] = useState(null);
    const [refunding, setRefunding] = useState(false);

    useEffect(() => {
        loadProducts();
    }, []);

    async function loadProducts() {
        try {
            setLoading(true);

            const data = await getProducts();

            setProducts(data);
        } catch (error) {
            console.error(error);
            setMessage("Failed to load products.");
        } finally {
            setLoading(false);
        }
    }

    async function handleAddProduct(product) {

        const quantityInOrder = Number(
            order?.items?.find(
                (item) => item.product?.id === product.id
            )?.quantity || 0
        );
        const availableStock = product.trackStock === true
            ? Math.max(0, Number(product.currentStock ?? 0))
            : null;

        if (availableStock !== null
            && quantityInOrder + 1 > availableStock) {
            setMessage(
                `${product.name} does not have enough stock available.`
            );
            return;
        }

        try {

            setAddingProductId(product.id);
            setMessage("");

            let currentOrder = order;

            /*
             * No open cart yet:
             * create one.
             */
            if (!currentOrder) {

                currentOrder =
                    await createOrder(
                        attachedSession?.id ?? null
                    );
            }

            /*
             * Add product to current order.
             */
            const updated =
                await addOrderItem(
                    currentOrder.id,
                    product.id,
                    1
                );

            setOrder(updated);

        } catch (error) {

            console.error(error);

            setMessage(
                error.message ||
                "Could not add product."
            );

        } finally {

            setAddingProductId(null);
        }
    }

    async function handleRemove(itemId) {
        if (!order) return;

        try {
            const updated = await removeOrderItem(order.id, itemId);

            /*
             * Removing the last item from a standalone order cancels that
             * order on the backend. Do not keep the cancelled order in state:
             * the next product must create a fresh open order.
             *
             * Session-attached orders stay open when they become empty, so
             * they can continue to receive products for the same session.
             */
            setOrder(
                updated?.status === "OPEN"
                    ? updated
                    : null
            );
            setMessage("");
        } catch (error) {
            console.error(error);
            setMessage("Could not remove product.");
        }
    }

    async function handleComplete() {
        if (!order) return;

        try {
            setCompleting(true);

            const completed = await completeOrder(order.id);

            setMessage(`Bill ${completed.billNumber} completed — ${Number(completed.totalAmount).toFixed(2)} EGP`);
            setReceipt(completed);

            setOrder(null);
        } catch (error) {
            console.error(error);
            setMessage("Could not complete order.");
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
            setMessage(error.message || "Could not refund bill.");
        } finally {
            setRefunding(false);
        }
    }

    return (<div className="pos-page">
        <div className="pos-header">

            <div>
    <span className="page-label">
      POINT OF SALE
    </span>

                <h2>Products</h2>

                <p>
                    Sell drinks and snacks or attach
                    them to a gaming session.
                </p>
            </div>

            {attachedSession && (
                <div className="attached-session-card">


      <span className="attached-label">
        ATTACHED TO
      </span>

                    <strong>
                        {attachedSession.device?.name}
                    </strong>

                    <small>
                        Session #{attachedSession.id}
                        {" · "}
                        {attachedSession.sessionType}
                    </small>

                    <small>
                        {Number(
                            attachedSession.unitPriceSnapshot || 0
                        ).toFixed(2)}
                        {" "}
                        EGP
                        {attachedSession.sessionType === "MATCH"
                            ? " / match"
                            : " / hour"}
                    </small>
                </div>
            )}

        </div>

        {message && (<div className="pricing-message">
            {message}
        </div>)}

        <div className="pos-layout">
            <section className="products-panel">
                {loading ? (<p>Loading products...</p>) : (<div className="product-grid">
                    {products.map((product) => (<ProductCard
                        key={product.id}
                        product={product}
                        disabled={addingProductId === product.id}
                        quantityInOrder={Number(
                            order?.items?.find(
                                (item) => item.product?.id === product.id
                            )?.quantity || 0
                        )}
                        onAdd={handleAddProduct}
                    />))}
                </div>)}
            </section>

            <OrderCart
                order={order}
                attachedSession={attachedSession}
                onRemove={handleRemove}
                onComplete={handleComplete}
                completing={completing}
            />
        </div>
        {receipt && (
            <ReceiptModal
                bill={receipt}
                refunding={refunding}
                onClose={() => setReceipt(null)}
                onRefund={handleRefund}
            />
        )}
    </div>);
}
