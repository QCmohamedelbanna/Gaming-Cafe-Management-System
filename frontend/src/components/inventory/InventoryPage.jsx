import { useEffect, useMemo, useState } from "react";
import { useLanguage } from "../../i18n";

import {
    adjustStock,
    getInventoryCategories,
    getInventoryProducts,
    getStockMovements,
    purchaseStock,
    recordWaste,
} from "../../api/inventoryApi";
import {
    createProduct,
    deleteProduct,
    setProductActive,
    updateProduct,
} from "../../api/productApi";
import ProductFormModal from "../products/ProductFormModal";
import DeleteProductModal from "../products/DeleteProductModal";
import StockMovementModal from "./StockMovementModal";

export default function InventoryPage() {
    const { t, formatCurrency, formatNumber, language } = useLanguage();
    const [products, setProducts] = useState([]);
    const [categories, setCategories] = useState([]);
    const [movements, setMovements] = useState([]);
    const [loading, setLoading] = useState(true);
    const [movementLoading, setMovementLoading] = useState(false);
    const [busyId, setBusyId] = useState(null);
    const [saving, setSaving] = useState(false);
    const [formProduct, setFormProduct] = useState(undefined);
    const [deleteTarget, setDeleteTarget] = useState(null);
    const [deleteError, setDeleteError] = useState("");
    const [movementTarget, setMovementTarget] = useState(null);
    const [movementMode, setMovementMode] = useState(null);
    const [ledgerProduct, setLedgerProduct] = useState(null);
    const [search, setSearch] = useState("");
    const [categoryFilter, setCategoryFilter] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    async function loadInventory() {
        try {
            setLoading(true);
            setError("");
            const [productData, categoryData, movementData] = await Promise.all([
                getInventoryProducts(),
                getInventoryCategories(),
                getStockMovements(),
            ]);
            setProducts(productData || []);
            setCategories(categoryData || []);
            setMovements(movementData || []);
        } catch (loadError) {
            console.error(loadError);
            setError(loadError.message || t("inventory.loadError"));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadInventory();
    }, []);

    const visibleProducts = useMemo(() => {
        const normalizedSearch = search.trim().toLowerCase();
        return products.filter((product) => {
            const matchesSearch = !normalizedSearch
                || product.name?.toLowerCase().includes(normalizedSearch)
                || product.sku?.toLowerCase().includes(normalizedSearch)
                || product.category?.toLowerCase().includes(normalizedSearch);
            const matchesCategory = !categoryFilter
                || product.category === categoryFilter;
            return matchesSearch && matchesCategory;
        });
    }, [products, search, categoryFilter]);

    const lowStockCount = products.filter((product) => product.lowStock).length;
    const trackedCount = products.filter((product) => product.trackStock).length;
    const stockValue = products.reduce((total, product) => total + (
        product.trackStock
            ? Number(product.currentStock || 0) * Number(product.costPrice || 0)
            : 0
    ), 0);

    function showSuccess(text) {
        setError("");
        setMessage(text);
    }

    function replaceProduct(saved) {
        setProducts((current) => {
            const exists = current.some((item) => item.id === saved.id);
            const next = exists
                ? current.map((item) => item.id === saved.id ? saved : item)
                : [...current, saved];
            return next.sort((a, b) => a.name.localeCompare(b.name));
        });
    }

    async function handleSaveProduct(data) {
        try {
            setSaving(true);
            setError("");
            const saved = formProduct
                ? await updateProduct(formProduct.id, data)
                : await createProduct(data);
            replaceProduct(saved);
            setFormProduct(undefined);
            await refreshCategories();
            showSuccess(t("products.saved", { name: saved.name }));
        } catch (saveError) {
            console.error(saveError);
            setError(saveError.message || t("products.saveError"));
        } finally {
            setSaving(false);
        }
    }

    async function refreshCategories() {
        try {
            setCategories(await getInventoryCategories());
        } catch (categoryError) {
            console.error(categoryError);
        }
    }

    async function handleToggle(product) {
        try {
            setBusyId(product.id);
            setMessage("");
            setError("");
            const updated = await setProductActive(product.id, !product.active);
            replaceProduct(updated);
            showSuccess(updated.active
                ? t("products.activated", { name: updated.name })
                : t("products.deactivated", { name: updated.name }));
        } catch (toggleError) {
            console.error(toggleError);
            setError(toggleError.message || t("products.statusError"));
        } finally {
            setBusyId(null);
        }
    }

    async function handleDelete(product) {
        try {
            setBusyId(product.id);
            setError("");
            await deleteProduct(product.id);
            setProducts((current) => current.filter((item) => item.id !== product.id));
            setDeleteTarget(null);
            showSuccess(t("products.deleted", { name: product.name }));
        } catch (deleteErrorValue) {
            console.error(deleteErrorValue);
            const messageValue = deleteErrorValue.message || t("products.deleteError");
            setDeleteError(messageValue);
            setError(messageValue);
        } finally {
            setBusyId(null);
        }
    }

    async function handleStockSave(data) {
        if (!movementTarget || !movementMode) return;
        try {
            setSaving(true);
            setError("");
            const updated = movementMode === "purchase"
                ? await purchaseStock(movementTarget.id, data)
                : movementMode === "waste"
                    ? await recordWaste(movementTarget.id, data)
                    : await adjustStock(movementTarget.id, data);
            replaceProduct(updated);
            setMovementTarget(null);
            setMovementMode(null);
            await refreshMovements(ledgerProduct?.id ?? null);
            showSuccess(
                movementMode === "purchase"
                    ? t("inventory.movementPurchase", { name: updated.name })
                    : movementMode === "waste"
                        ? t("inventory.movementWaste", { name: updated.name })
                        : t("inventory.movementAdjustment", { name: updated.name })
            );
        } catch (stockError) {
            console.error(stockError);
            setError(stockError.message || t("inventory.stockSaveError"));
        } finally {
            setSaving(false);
        }
    }

    async function refreshMovements(productId = null) {
        try {
            setMovementLoading(true);
            setMovements(await getStockMovements(productId));
        } catch (movementError) {
            console.error(movementError);
            setError(movementError.message || t("inventory.ledgerLoadError"));
        } finally {
            setMovementLoading(false);
        }
    }

    async function showLedger(product) {
        setLedgerProduct(product);
        await refreshMovements(product.id);
    }

    function showAllMovements() {
        setLedgerProduct(null);
        refreshMovements();
    }

    return (
        <div className="inventory-page">
            <div className="inventory-header">
                <div>
                    <span className="page-label">{t("inventory.adminInventory")}</span>
                    <h1>{t("inventory.title")}</h1>
                    <p>{t("inventory.descriptionShort")}</p>
                </div>
                <button
                    type="button"
                    className="product-add-button"
                    onClick={() => setFormProduct(null)}
                >
                    + {t("inventory.addProductAction")}
                </button>
            </div>

            {message && <div className="pricing-message">{message}</div>}
            {error && <div className="product-error-message">{error}</div>}

            <div className="inventory-summary">
                <div className="inventory-stat">
                    <span>{t("inventory.totalProducts")}</span>
                    <strong>{formatNumber(products.length)}</strong>
                    <small>{t("inventory.tracking", { count: formatNumber(trackedCount) })}</small>
                </div>
                <div className={`inventory-stat ${lowStockCount ? "warning" : ""}`}>
                    <span>{t("inventory.lowStock")}</span>
                    <strong>{formatNumber(lowStockCount)}</strong>
                    <small>{lowStockCount ? t("inventory.needsAttention") : t("inventory.allLevelsGood")}</small>
                </div>
                <div className="inventory-stat">
                    <span>{t("inventory.categories")}</span>
                    <strong>{formatNumber(categories.length)}</strong>
                    <small>{t("inventory.productGroups")}</small>
                </div>
                <div className="inventory-stat">
                    <span>{t("inventory.stockValue")}</span>
                    <strong>{formatCurrency(stockValue)}</strong>
                    <small>{t("inventory.trackedOnly")}</small>
                </div>
            </div>

            <div className="inventory-toolbar">
                <label className="inventory-search">
                    <span>{t("inventory.searchProducts")}</span>
                    <input
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder={t("inventory.searchPlaceholder")}
                    />
                </label>
                <label className="inventory-filter">
                    <span>{t("inventory.category")}</span>
                    <select
                        value={categoryFilter}
                        onChange={(event) => setCategoryFilter(event.target.value)}
                    >
                        <option value="">{t("inventory.allCategories")}</option>
                        {categories.map((category) => (
                            <option key={category} value={category}>{category}</option>
                        ))}
                    </select>
                </label>
                {(search || categoryFilter) && (
                    <button
                        type="button"
                        className="inventory-clear-button"
                        onClick={() => {
                            setSearch("");
                            setCategoryFilter("");
                        }}
                    >
                        {t("inventory.clearFiltersAction")}
                    </button>
                )}
            </div>

            {loading ? (
                <p>{t("inventory.loading")}</p>
            ) : visibleProducts.length === 0 ? (
                <div className="products-empty-state">
                    <h2>{products.length ? t("inventory.noMatchingProducts") : t("inventory.noProducts")}</h2>
                    <p>{products.length ? t("inventory.trySearch") : t("inventory.noProductsStart")}</p>
                </div>
            ) : (
                <div className="products-table-wrap inventory-table-wrap">
                    <table className="products-table inventory-table">
                        <thead>
                            <tr>
                                <th>{t("inventory.product")}</th>
                                <th>{t("inventory.category")}</th>
                                <th>{t("inventory.sellingCost")}</th>
                                <th>{t("inventory.stock")}</th>
                                <th>{t("inventory.status")}</th>
                                <th><span className="sr-only">{t("common.actions")}</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {visibleProducts.map((product) => {
                                const busy = busyId === product.id;
                                const lowStock = product.trackStock && product.lowStock;
                                return (
                                    <tr key={product.id} className={lowStock ? "low-stock-row" : ""}>
                                        <td>
                                            <strong>{product.name}</strong>
                                            <small className="inventory-sku">{product.sku || t("inventory.noSkuBarcode")}</small>
                                        </td>
                                        <td>{product.category || t("inventory.uncategorized")}</td>
                                        <td>
                                            <strong>{formatCurrency(product.sellingPrice ?? product.price ?? 0)}</strong>
                                            <small className="inventory-muted">{t("inventory.cost", { value: formatCurrency(product.costPrice || 0) })}</small>
                                        </td>
                                        <td>
                                            {product.trackStock ? (
                                                <>
                                                    <strong className={lowStock ? "low-stock-value" : ""}>
                                                        {formatNumber(product.currentStock || 0, { maximumFractionDigits: 3 })} {product.unit || t("form.unit")}
                                                    </strong>
                                                    <small className="inventory-muted">{t("inventory.min", { value: formatNumber(product.minimumStock || 0, { maximumFractionDigits: 3 }) })}</small>
                                                </>
                                            ) : (
                                                <span className="inventory-muted">{t("inventory.notTracked")}</span>
                                            )}
                                        </td>
                                        <td>
                                            <span className={product.active ? "product-status active" : "product-status inactive"}>
                                                {product.active ? t("products.active") : t("products.inactive")}
                                            </span>
                                            {lowStock && <span className="low-stock-badge">{t("inventory.lowBadge")}</span>}
                                        </td>
                                        <td>
                                            <div className="product-row-actions inventory-row-actions">
                                            <button disabled={busy} onClick={() => setFormProduct(product)}>{t("common.edit")}</button>
                                                <button
                                                    disabled={busy || !product.trackStock}
                                                    title={product.trackStock ? t("inventory.recordPurchase") : t("inventory.enableTracking")}
                                                    onClick={() => {
                                                        setMovementTarget(product);
                                                        setMovementMode("purchase");
                                                    }}
                                                >
                                                    {t("inventory.purchase")}
                                                </button>
                                                <button
                                                    disabled={busy || !product.trackStock}
                                                    title={product.trackStock ? t("inventory.adjustStock") : t("inventory.enableTracking")}
                                                    onClick={() => {
                                                        setMovementTarget(product);
                                                        setMovementMode("adjustment");
                                                    }}
                                                >
                                                    {t("inventory.adjust")}
                                                </button>
                                                <button
                                                    disabled={busy || !product.trackStock}
                                                    title={product.trackStock ? t("inventory.recordWaste") : t("inventory.enableTracking")}
                                                    onClick={() => {
                                                        setMovementTarget(product);
                                                        setMovementMode("waste");
                                                    }}
                                                >
                                                    {t("inventory.waste")}
                                                </button>
                                                <button disabled={busy} onClick={() => showLedger(product)}>{t("inventory.ledger")}</button>
                                                <button disabled={busy} onClick={() => handleToggle(product)}>
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

            <section className="inventory-ledger-section">
                <div className="inventory-section-header">
                    <div>
                        <span className="page-label">{t("inventory.auditLabel")}</span>
                        <h2>{ledgerProduct ? t("inventory.ledgerTitle", { name: ledgerProduct.name }) : t("inventory.recentStockActivity")}</h2>
                    </div>
                    {ledgerProduct && (
                        <button type="button" className="product-secondary-button" onClick={showAllMovements}>
                            {t("inventory.showAllMovements")}
                        </button>
                    )}
                </div>
                {movementLoading ? (
                    <p>{t("inventory.loadingMovements")}</p>
                ) : movements.length === 0 ? (
                    <p className="inventory-empty-ledger">{t("inventory.noMovements")}</p>
                ) : (
                    <div className="inventory-movement-list">
                        {movements.map((movement) => (
                            <div className="inventory-movement-row" key={movement.id}>
                                <div>
                                    <strong>{movement.productName}</strong>
                                    <small>{movement.type === "PURCHASE" ? t("inventory.purchase") : movement.type === "SALE" ? t("common.sale") : movement.type === "RETURN" ? t("common.return") : movement.type === "ADJUSTMENT" ? t("common.adjustment") : t("common.waste")} · {movement.reference || t("inventory.noReference")}</small>
                                </div>
                                <strong className={Number(movement.quantity) < 0 ? "movement-negative" : "movement-positive"}>
                                    {Number(movement.quantity) > 0 ? "+" : ""}{Number(movement.quantity).toFixed(3)}
                                </strong>
                                <div className="inventory-movement-meta">
                                    <span>{movement.createdBy}</span>
                                    <small>{formatDate(movement.createdAt, language)}</small>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {formProduct !== undefined && (
                <ProductFormModal
                    product={formProduct}
                    saving={saving}
                    onClose={() => setFormProduct(undefined)}
                    onSave={handleSaveProduct}
                />
            )}

            {movementTarget && movementMode && (
                <StockMovementModal
                    product={movementTarget}
                    mode={movementMode}
                    saving={saving}
                    onClose={() => {
                        if (saving) return;
                        setMovementTarget(null);
                        setMovementMode(null);
                    }}
                    onSave={handleStockSave}
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

function formatDate(value, language = "en") {
    if (!value) return "—";
    return new Date(value).toLocaleString(language === "ar" ? "ar-EG" : "en-EG", {
        dateStyle: "medium",
        timeStyle: "short",
    });
}
