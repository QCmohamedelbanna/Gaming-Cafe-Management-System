import { useLanguage } from "../../i18n";

export default function ProductCard({
                                        product,
                                        onAdd,
                                        disabled,
                                        quantityInOrder = 0,
}) {
    const { t, formatCurrency } = useLanguage();
    const trackStock = product.trackStock === true;
    const availableStock = trackStock
        ? Math.max(0, Number(product.currentStock ?? 0))
        : null;
    const stockLimitReached = availableStock !== null
        && Number(quantityInOrder) + 1 > availableStock;
    const stockLabel = !trackStock
        ? `+ ${t("pos.add")}`
        : stockLimitReached
            ? availableStock <= 0 ? t("pos.outOfStock") : t("pos.stockLimitReached")
            : t("pos.available", { count: formatStockQuantity(availableStock - Number(quantityInOrder)) });

    return (
        <button
            className={`product-card${stockLimitReached ? " out-of-stock" : ""}`}
            disabled={disabled || stockLimitReached}
            title={stockLimitReached ? stockLabel : undefined}
            onClick={() => onAdd(product)}
        >
            <div className="product-icon">
                {getProductIcon(product.name)}
            </div>

            <strong>{product.name}</strong>

            <span>
        {formatCurrency(product.sellingPrice ?? product.price ?? 0)}
      </span>

            <small>{stockLabel}</small>
        </button>
    );
}

function formatStockQuantity(value) {
    return Number.isInteger(value)
        ? String(value)
        : value.toFixed(3).replace(/\.?0+$/, "");
}

function getProductIcon(name) {
    const value = name.toLowerCase();

    if (value.includes("tea")) return "☕";
    if (value.includes("coffee")) return "☕";
    if (value.includes("pepsi")) return "🥤";
    if (value.includes("water")) return "💧";
    if (value.includes("chips")) return "🍟";

    return "🛒";
}
