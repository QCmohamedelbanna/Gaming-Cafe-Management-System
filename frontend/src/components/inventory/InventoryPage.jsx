import { useEffect, useMemo, useState } from "react";

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
            setError(loadError.message || "Could not load inventory.");
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
            showSuccess(`${saved.name} saved successfully.`);
        } catch (saveError) {
            console.error(saveError);
            setError(saveError.message || "Could not save product.");
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
            showSuccess(`${updated.name} ${updated.active ? "activated" : "deactivated"}.`);
        } catch (toggleError) {
            console.error(toggleError);
            setError(toggleError.message || "Could not update product status.");
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
            showSuccess(`${product.name} deleted.`);
        } catch (deleteErrorValue) {
            console.error(deleteErrorValue);
            const messageValue = deleteErrorValue.message || "Could not delete product.";
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
                    ? `${updated.name} purchase recorded.`
                    : movementMode === "waste"
                        ? `${updated.name} waste recorded.`
                        : `${updated.name} stock adjusted.`
            );
        } catch (stockError) {
            console.error(stockError);
            setError(stockError.message || "Could not save stock movement.");
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
            setError(movementError.message || "Could not load stock ledger.");
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
                    <span className="page-label">ADMIN · INVENTORY</span>
                    <h1>Inventory</h1>
                    <p>Track products, stock balances, and the movement ledger.</p>
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

            <div className="inventory-summary">
                <div className="inventory-stat">
                    <span>Total products</span>
                    <strong>{products.length}</strong>
                    <small>{trackedCount} tracking stock</small>
                </div>
                <div className={`inventory-stat ${lowStockCount ? "warning" : ""}`}>
                    <span>Low stock</span>
                    <strong>{lowStockCount}</strong>
                    <small>{lowStockCount ? "Needs attention" : "All levels look good"}</small>
                </div>
                <div className="inventory-stat">
                    <span>Categories</span>
                    <strong>{categories.length}</strong>
                    <small>Product groups</small>
                </div>
                <div className="inventory-stat">
                    <span>Stock at cost</span>
                    <strong>{stockValue.toFixed(2)} EGP</strong>
                    <small>Tracked items only</small>
                </div>
            </div>

            <div className="inventory-toolbar">
                <label className="inventory-search">
                    <span>Search products</span>
                    <input
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Name, SKU, or category"
                    />
                </label>
                <label className="inventory-filter">
                    <span>Category</span>
                    <select
                        value={categoryFilter}
                        onChange={(event) => setCategoryFilter(event.target.value)}
                    >
                        <option value="">All categories</option>
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
                        Clear filters
                    </button>
                )}
            </div>

            {loading ? (
                <p>Loading inventory...</p>
            ) : visibleProducts.length === 0 ? (
                <div className="products-empty-state">
                    <h2>{products.length ? "No matching products" : "No products yet"}</h2>
                    <p>{products.length ? "Try a different search or category." : "Add a product to start tracking inventory."}</p>
                </div>
            ) : (
                <div className="products-table-wrap inventory-table-wrap">
                    <table className="products-table inventory-table">
                        <thead>
                            <tr>
                                <th>Product</th>
                                <th>Category</th>
                                <th>Selling / cost</th>
                                <th>Stock</th>
                                <th>Status</th>
                                <th><span className="sr-only">Actions</span></th>
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
                                            <small className="inventory-sku">{product.sku || "No SKU / barcode"}</small>
                                        </td>
                                        <td>{product.category || "Uncategorized"}</td>
                                        <td>
                                            <strong>{Number(product.sellingPrice ?? product.price ?? 0).toFixed(2)} EGP</strong>
                                            <small className="inventory-muted">Cost {Number(product.costPrice || 0).toFixed(2)} EGP</small>
                                        </td>
                                        <td>
                                            {product.trackStock ? (
                                                <>
                                                    <strong className={lowStock ? "low-stock-value" : ""}>
                                                        {Number(product.currentStock || 0).toFixed(3)} {product.unit || "unit"}
                                                    </strong>
                                                    <small className="inventory-muted">Min {Number(product.minimumStock || 0).toFixed(3)}</small>
                                                </>
                                            ) : (
                                                <span className="inventory-muted">Not tracked</span>
                                            )}
                                        </td>
                                        <td>
                                            <span className={product.active ? "product-status active" : "product-status inactive"}>
                                                {product.active ? "ACTIVE" : "INACTIVE"}
                                            </span>
                                            {lowStock && <span className="low-stock-badge">LOW STOCK</span>}
                                        </td>
                                        <td>
                                            <div className="product-row-actions inventory-row-actions">
                                                <button disabled={busy} onClick={() => setFormProduct(product)}>Edit</button>
                                                <button
                                                    disabled={busy || !product.trackStock}
                                                    title={product.trackStock ? "Record a purchase" : "Enable stock tracking first"}
                                                    onClick={() => {
                                                        setMovementTarget(product);
                                                        setMovementMode("purchase");
                                                    }}
                                                >
                                                    Purchase
                                                </button>
                                                <button
                                                    disabled={busy || !product.trackStock}
                                                    title={product.trackStock ? "Adjust stock" : "Enable stock tracking first"}
                                                    onClick={() => {
                                                        setMovementTarget(product);
                                                        setMovementMode("adjustment");
                                                    }}
                                                >
                                                    Adjust
                                                </button>
                                                <button
                                                    disabled={busy || !product.trackStock}
                                                    title={product.trackStock ? "Record wasted stock" : "Enable stock tracking first"}
                                                    onClick={() => {
                                                        setMovementTarget(product);
                                                        setMovementMode("waste");
                                                    }}
                                                >
                                                    Waste
                                                </button>
                                                <button disabled={busy} onClick={() => showLedger(product)}>Ledger</button>
                                                <button disabled={busy} onClick={() => handleToggle(product)}>
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

            <section className="inventory-ledger-section">
                <div className="inventory-section-header">
                    <div>
                        <span className="page-label">AUDIT TRAIL</span>
                        <h2>{ledgerProduct ? `${ledgerProduct.name} ledger` : "Recent stock activity"}</h2>
                    </div>
                    {ledgerProduct && (
                        <button type="button" className="product-secondary-button" onClick={showAllMovements}>
                            Show all movements
                        </button>
                    )}
                </div>
                {movementLoading ? (
                    <p>Loading stock movements...</p>
                ) : movements.length === 0 ? (
                    <p className="inventory-empty-ledger">No stock movements recorded yet.</p>
                ) : (
                    <div className="inventory-movement-list">
                        {movements.map((movement) => (
                            <div className="inventory-movement-row" key={movement.id}>
                                <div>
                                    <strong>{movement.productName}</strong>
                                    <small>{movement.type} · {movement.reference || "No reference"}</small>
                                </div>
                                <strong className={Number(movement.quantity) < 0 ? "movement-negative" : "movement-positive"}>
                                    {Number(movement.quantity) > 0 ? "+" : ""}{Number(movement.quantity).toFixed(3)}
                                </strong>
                                <div className="inventory-movement-meta">
                                    <span>{movement.createdBy}</span>
                                    <small>{formatDate(movement.createdAt)}</small>
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

function formatDate(value) {
    if (!value) return "—";
    return new Date(value).toLocaleString([], {
        dateStyle: "medium",
        timeStyle: "short",
    });
}
