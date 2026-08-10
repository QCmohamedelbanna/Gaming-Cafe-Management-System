import {useEffect, useState} from "react";

import {getProducts} from "../../api/productApi";

import {
    createOrder, addOrderItem, removeOrderItem, completeOrder,
} from "../../api/orderApi";

import ProductCard from "./ProductCard";
import OrderCart from "./OrderCart";

export default function POSPage({
                                    attachedSession = null,
                                }) {
    const [products, setProducts] = useState([]);
    const [order, setOrder] = useState(null);

    const [loading, setLoading] = useState(true);
    const [addingProductId, setAddingProductId] = useState(null);

    const [completing, setCompleting] = useState(false);

    const [message, setMessage] = useState("");

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

            setOrder(updated);
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

            setMessage(`Order #${completed.id} completed — ${Number(completed.totalAmount).toFixed(2)} EGP`);

            setOrder(null);
        } catch (error) {
            console.error(error);
            setMessage("Could not complete order.");
        } finally {
            setCompleting(false);
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
    </div>);
}