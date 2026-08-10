export default function OrderCart({
                                      order,
                                      attachedSession,
                                      onRemove,
                                      onComplete,
                                      completing,
                                  }) {
    if (!order) {
        return (
            <div className="order-cart">
                <h2>Current Order</h2>
                <p className="empty-cart">
                    Select a product to start an order.
                </p>
            </div>
        );
    }

    return (
        <div className="order-cart">
            <div className="cart-header">
                <div>
          <span className="page-label">
            CURRENT ORDER
          </span>

                    <h2>Order #{order.id}</h2>
                    {attachedSession && (
                        <small className="cart-session-reference">
                            {attachedSession.device?.name}
                            {" · "}
                            {attachedSession.sessionType}
                            {" · "}
                            Session #{attachedSession.id}
                        </small>
                    )}
                </div>

                <span className="order-status">
          {order.status}
        </span>
            </div>

            <div className="cart-items">
                {order.items?.length === 0 && (
                    <p className="empty-cart">
                        No products added yet.
                    </p>
                )}

                {order.items?.map((item) => (
                    <div
                        className="cart-item"
                        key={item.id}
                    >
                        <div className="cart-item-info">
                            <strong>
                                {item.product.name}
                            </strong>

                            <span>
                {item.quantity} ×{" "}
                                {Number(
                                    item.unitPriceSnapshot
                                ).toFixed(2)}{" "}
                                EGP
              </span>
                        </div>

                        <div className="cart-item-right">
                            <strong>
                                {Number(item.lineTotal).toFixed(2)}{" "}
                                EGP
                            </strong>

                            <button
                                className="remove-item"
                                onClick={() =>
                                    onRemove(item.id)
                                }
                            >
                                ×
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            <div className="cart-total">
                <span>Total</span>

                <strong>
                    {Number(
                        order.totalAmount || 0
                    ).toFixed(2)}{" "}
                    EGP
                </strong>
            </div>

            <button
                className="checkout-button"
                disabled={
                    completing ||
                    !order.items ||
                    order.items.length === 0
                }
                onClick={onComplete}
            >
                {completing
                    ? "Completing..."
                    : "Complete Order"}
            </button>
        </div>
    );
}