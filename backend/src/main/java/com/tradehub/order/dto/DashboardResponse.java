package com.tradehub.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalOrders,
        long paidOrders,
        long cancelledOrders,
        BigDecimal totalRevenue,
        BigDecimal paidRevenue,
        BigDecimal averageOrderValue,
        List<TopProductResponse> topProducts,
        List<OrderResponse> recentOrders
) {
}