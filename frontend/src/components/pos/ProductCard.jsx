export default function ProductCard({
                                        product,
                                        onAdd,
                                        disabled,
                                    }) {
    return (
        <button
            className="product-card"
            disabled={disabled}
            onClick={() => onAdd(product)}
        >
            <div className="product-icon">
                {getProductIcon(product.name)}
            </div>

            <strong>{product.name}</strong>

            <span>
        {Number(product.price).toFixed(2)} EGP
      </span>

            <small>+ Add</small>
        </button>
    );
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