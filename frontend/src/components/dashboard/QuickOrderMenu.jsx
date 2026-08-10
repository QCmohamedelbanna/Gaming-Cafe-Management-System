import { useState } from "react";

export default function QuickOrderMenu({
                                           products,
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

                    {products.map(product => (

                        <button
                            type="button"
                            key={product.id}
                            className="quick-product-item"
                            disabled={
                                addingProductId === product.id
                            }
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
                                        product.price
                                    ).toFixed(2)}
                                    {" "}
                                    EGP
                                </span>
                            </div>

                            <strong>
                                +
                            </strong>

                        </button>

                    ))}

                </div>

            )}

        </div>
    );
}