import { useState } from "react";
import { useLanguage } from "../../i18n";

export default function QuickOrderMenu({
                                           products,
                                           order,
                                           onAddProduct,
                                       addingProductId,
                                       }) {
    const { t, formatCurrency, formatNumber } = useLanguage();

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
                🛒 {t("quickOrder.addOrder")}
                <span>
                    {open ? " ▲" : " ▼"}
                </span>
            </button>

            {open && (

                <div className="quick-order-menu">

                    <div className="quick-order-title">
                        {t("quickOrder.addProduct")}
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
                                    ? t("quickOrder.outOfStock")
                                    : t("quickOrder.stockLimitReached")
                                : t("quickOrder.available", {
                                    count: formatStockQuantity(availableStock - quantityInOrder),
                                });

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
                                    {formatCurrency(product.sellingPrice ?? product.price ?? 0)}
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
