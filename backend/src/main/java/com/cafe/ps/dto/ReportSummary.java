package com.cafe.ps.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportSummary(
        LocalDate from,
        LocalDate to,
        BigDecimal gamingRevenue,
        BigDecimal cafeRevenue,
        BigDecimal totalRevenue,
        BigDecimal discountAmount,
        long completedBills,
        BigDecimal averageBillValue,
        long cancelledBills,
        BigDecimal cancelledAmount,
        long refundedBills,
        BigDecimal refundedAmount,
        List<PaymentMethodTotal> paymentMethods,
        List<RevenueBreakdown> revenueByDevice,
        List<RevenueBreakdown> revenueBySessionType,
        List<ProductSalesReport> productSales,
        List<CashierShiftSummary> cashierShifts
) {
}
