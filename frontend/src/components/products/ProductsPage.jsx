import { useEffect, useState } from "react";
import { useLanguage } from "../../i18n";
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
    const { t, formatCurrency } = useLanguage();
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
            setError(loadError.message || t("products.loadError"));
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
            showSuccess(t("products.saved", { name: saved.name }));
        } catch (saveError) {
            console.error(saveError);
            setError(saveError.message || t("products.saveError"));
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
                updated.active
                    ? t("products.activated", { name: updated.name })
                    : t("products.deactivated", { name: updated.name })
            );
            return true;
        } catch (toggleError) {
            console.error(toggleError);
            setError(toggleError.message || t("products.statusError"));
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
            showSuccess(t("products.deleted", { name: product.name }));
        } catch (deleteError) {
            console.error(deleteError);
            const message = deleteError.message || t("products.deleteError");
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
                    <span className="page-label">{t("products.pageLabel")}</span>
                    <h1>{t("products.title")}</h1>
                    <p>{t("products.descriptionShort")}</p>
                </div>
                <button
                    type="button"
                    className="product-add-button"
                    onClick={() => setFormProduct(null)}
                >
                    + {t("products.addProduct")}
                </button>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            {loading ? (
                <p>{t("products.loading")}</p>
            ) : products.length === 0 ? (
                <div className="products-empty-state">
                    <h2>{t("products.noProducts")}</h2>
                    <p>{t("products.addFirst")}</p>
                </div>
            ) : (
                <div className="products-table-wrap">
                    <table className="products-table">
                        <thead>
                            <tr>
                                <th>{t("products.name")}</th>
                                <th>{t("products.price")}</th>
                                <th>{t("products.status")}</th>
                                <th><span className="sr-only">{t("common.actions")}</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {products.map((product) => {
                                const busy = busyId === product.id;
                                return (
                                    <tr key={product.id}>
                                        <td><strong>{product.name}</strong></td>
                                        <td>{formatCurrency(product.sellingPrice ?? product.price ?? 0)}</td>
                                        <td>
                                            <span className={product.active
                                                ? "product-status active"
                                                : "product-status inactive"}
                                            >
                                                {product.active ? t("products.active") : t("products.inactive")}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="product-row-actions">
                                                <button
                                                    disabled={busy}
                                                    onClick={() => setFormProduct(product)}
                                                >
                                                    {t("common.edit")}
                                                </button>
                                                <button
                                                    disabled={busy}
                                                    onClick={() => handleToggle(product)}
                                                >
                                                    {product.active ? t("products.deactivate") : t("products.activate")}
                                                </button>
                                                <button
                                                    className="product-delete-button"
                                                    disabled={busy}
                                                    onClick={() => {
                                                        setDeleteError("");
                                                        setDeleteTarget(product);
                                                    }}
                                                >
                                                    {busy ? t("common.working") : t("common.delete")}
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
