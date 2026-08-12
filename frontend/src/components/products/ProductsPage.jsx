import { useEffect, useState } from "react";
import {
    createProduct,
    deleteProduct,
    getAdminProducts,
    setProductActive,
    updateProduct,
} from "../../api/productApi";
import ProductFormModal from "./ProductFormModal";
import DeleteProductModal from "./DeleteProductModal";

export default function ProductsPage() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [formProduct, setFormProduct] = useState(undefined);
    const [saving, setSaving] = useState(false);
    const [deleteTarget, setDeleteTarget] = useState(null);
    const [deleteError, setDeleteError] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    async function loadProducts() {
        try {
            setLoading(true);
            setError("");
            setProducts(await getAdminProducts());
        } catch (loadError) {
            console.error(loadError);
            setError(loadError.message || "Could not load products.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadProducts();
    }, []);

    function showSuccess(text) {
        setError("");
        setMessage(text);
    }

    async function handleSave(data) {
        try {
            setSaving(true);
            setError("");
            const saved = formProduct
                ? await updateProduct(formProduct.id, data)
                : await createProduct(data);

            setProducts((current) => {
                const next = formProduct
                    ? current.map((item) => item.id === saved.id ? saved : item)
                    : [...current, saved];
                return next.sort((a, b) => a.name.localeCompare(b.name));
            });
            setFormProduct(undefined);
            showSuccess(`${saved.name} saved successfully.`);
        } catch (saveError) {
            console.error(saveError);
            setError(saveError.message || "Could not save product.");
        } finally {
            setSaving(false);
        }
    }

    async function handleToggle(product) {
        try {
            setBusyId(product.id);
            setMessage("");
            setError("");
            const updated = await setProductActive(product.id, !product.active);
            setProducts((current) =>
                current.map((item) => item.id === updated.id ? updated : item)
            );
            showSuccess(
                `${updated.name} ${updated.active ? "activated" : "deactivated"}.`
            );
            return true;
        } catch (toggleError) {
            console.error(toggleError);
            setError(toggleError.message || "Could not update product status.");
            return false;
        } finally {
            setBusyId(null);
        }
    }

    async function handleDelete(product) {
        try {
            setBusyId(product.id);
            setMessage("");
            setError("");
            setDeleteError("");
            await deleteProduct(product.id);
            setProducts((current) =>
                current.filter((item) => item.id !== product.id)
            );
            setDeleteTarget(null);
            setDeleteError("");
            showSuccess(`${product.name} deleted.`);
        } catch (deleteError) {
            console.error(deleteError);
            const message = deleteError.message || "Could not delete product.";
            setDeleteError(message);
            setError(message);
        } finally {
            setBusyId(null);
        }
    }

    return (
        <div className="products-management-page">
            <div className="products-management-header">
                <div>
                    <span className="page-label">ADMIN</span>
                    <h1>Products</h1>
                    <p>Manage POS products, prices, and availability.</p>
                </div>
                <button
                    type="button"
                    className="product-add-button"
                    onClick={() => setFormProduct(null)}
                >
                    + Add Product
                </button>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            {loading ? (
                <p>Loading products...</p>
            ) : products.length === 0 ? (
                <div className="products-empty-state">
                    <h2>No products yet</h2>
                    <p>Add the first product to make it available in POS.</p>
                </div>
            ) : (
                <div className="products-table-wrap">
                    <table className="products-table">
                        <thead>
                            <tr>
                                <th>Product</th>
                                <th>Price</th>
                                <th>Status</th>
                                <th><span className="sr-only">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {products.map((product) => {
                                const busy = busyId === product.id;
                                return (
                                    <tr key={product.id}>
                                        <td><strong>{product.name}</strong></td>
                                        <td>{Number(product.price).toFixed(2)} EGP</td>
                                        <td>
                                            <span className={product.active
                                                ? "product-status active"
                                                : "product-status inactive"}
                                            >
                                                {product.active ? "ACTIVE" : "INACTIVE"}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="product-row-actions">
                                                <button
                                                    disabled={busy}
                                                    onClick={() => setFormProduct(product)}
                                                >
                                                    Edit
                                                </button>
                                                <button
                                                    disabled={busy}
                                                    onClick={() => handleToggle(product)}
                                                >
                                                    {product.active ? "Deactivate" : "Activate"}
                                                </button>
                                                <button
                                                    className="product-delete-button"
                                                    disabled={busy}
                                                    onClick={() => {
                                                        setDeleteError("");
                                                        setDeleteTarget(product);
                                                    }}
                                                >
                                                    {busy ? "Working..." : "Delete"}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            {formProduct !== undefined && (
                <ProductFormModal
                    product={formProduct}
                    saving={saving}
                    onClose={() => setFormProduct(undefined)}
                    onSave={handleSave}
                />
            )}

            {deleteTarget && (
                <DeleteProductModal
                    product={deleteTarget}
                    deleting={busyId === deleteTarget.id}
                    error={deleteError}
                    onClose={() => {
                        setDeleteTarget(null);
                        setDeleteError("");
                    }}
                    onConfirm={() => handleDelete(deleteTarget)}
                />
            )}
        </div>
    );
}
