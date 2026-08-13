package com.cafe.ps.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * The public selling price used by inventory and POS. The legacy price
     * column is kept in sync so existing orders and database rows continue to
     * work while the schema evolves.
     */
    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(length = 80, unique = true)
    private String sku;

    @Column(length = 80)
    @Builder.Default
    private String category = "Uncategorized";

    @Column(name = "cost_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "track_stock")
    @Builder.Default
    private Boolean trackStock = false;

    /** Cached balance; StockMovement is the source-of-truth ledger. */
    @Column(name = "current_stock", precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "minimum_stock", precision = 14, scale = 3)
    @Builder.Default
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @Column(length = 30)
    @Builder.Default
    private String unit = "unit";

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(
            nullable = false,
            columnDefinition = "boolean default false"
    )
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    @PreUpdate
    @PostLoad
    private void normalizeDefaults() {
        if (sellingPrice == null) sellingPrice = price;
        if (price == null) price = sellingPrice;
        if (category == null || category.isBlank()) category = "Uncategorized";
        if (costPrice == null) costPrice = BigDecimal.ZERO;
        if (trackStock == null) trackStock = false;
        if (currentStock == null) currentStock = BigDecimal.ZERO;
        if (minimumStock == null) minimumStock = BigDecimal.ZERO;
        if (unit == null || unit.isBlank()) unit = "unit";
        if (active == null) active = true;
        if (deleted == null) deleted = false;
    }

    @JsonIgnore
    public BigDecimal effectiveSellingPrice() {
        return sellingPrice != null ? sellingPrice : price;
    }

    public boolean isLowStock() {
        return Boolean.TRUE.equals(trackStock)
                && currentStock != null
                && minimumStock != null
                && currentStock.compareTo(minimumStock) <= 0;
    }
}
