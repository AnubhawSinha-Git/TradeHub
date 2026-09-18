package com.tradehub.order;

import java.math.BigDecimal;

public interface TopProductProjection {
    Long getProductId();

    String getProductName();

    Long getUnitsSold();

    BigDecimal getRevenue();
}