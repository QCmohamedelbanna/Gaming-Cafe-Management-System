import { useState } from "react";

export default function QuickOrderMenu({
                                           products,
                                           order,
                                           onAddProduct,
                                           addingProductId,
                                       }) {

    const [open, setOpen] =
        useState(false);

    return (
        <div className="quick-order">

            <button
                type="button"
                className="device-order-button"
                onClick={() =>
                    setOpen(!open)
                }
            >
                🛒 Add Order
                <span>
                    {open ? " ▲" : " ▼"}
                </span>
            </button>

            {open && (

                <div className="quick-order-menu">

                    <div className="quick-order-title">
                        Add Product
                    </div>

                    {products.map(product => {
                        const quantityInOrder = Number(
                            order?.items?.find(
                                (item) => item.product?.id === product.id
                            )?.quantity || 0
                        );
                        const availableStock = product.trackStock === true
                            ? Math.max(0, Number(product.currentStock ?? 0))
                            : null;
                        const stockLimitReached = availableStock !== null
                            && quantityInOrder + 1 > availableStock;
                        const stockLabel = product.trackStock !== true
                            ? null
                            : stockLimitReached
                                ? availableStock <= 0
                                    ? "Out of stock"
                                    : "Stock limit reached"
                                : `${formatStockQuantity(availableStock - quantityInOrder)} available`;

                        return (

                        <button
                            type="button"
                            key={product.id}
                            className={`quick-product-item${stockLimitReached ? " out-of-stock" : ""}`}
                            disabled={
                                addingProductId === product.id || stockLimitReached
                            }
                            title={stockLimitReached ? stockLabel : undefined}
                            onClick={async () => {
                                await onAddProduct(product);
                                setOpen(false);
                            }}
                        >

                            <div>
                                <strong>
                                    {product.name}
                                </strong>

                                <span>
                                    {Number(
                                        product.sellingPrice ?? product.price ?? 0
                                    ).toFixed(2)}
                                    {" "}
                                    EGP
                                </span>
                                {stockLabel && <small>{stockLabel}</small>}
                            </div>

                            <strong>
                                +
                            </strong>

                        </button>

                        );
                    })}

                </div>

            )}

        </div>
    );
}

function formatStockQuantity(value) {
    return Number.isInteger(value)
        ? String(value)
        : value.toFixed(3).replace(/\.?0+$/, "");
}
