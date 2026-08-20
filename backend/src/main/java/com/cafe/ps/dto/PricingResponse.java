package com.cafe.ps.dto;

import com.cafe.ps.entity.BillingUnit;
import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.Pricing;
import com.cafe.ps.entity.SessionType;

import java.math.BigDecimal;

public record PricingResponse(
        Long id,
        DeviceType deviceType,
        SessionType sessionType,
        BillingUnit billingUnit,
        BigDecimal price,
        Integer matchDurationMinutes,
        Integer warningBeforeExpiryMinutes,
        Boolean active
) {
    public static PricingResponse from(Pricing pricing) {
        return new PricingResponse(
                pricing.getId(),
                pricing.getDeviceType(),
                pricing.getSessionType(),
                pricing.getBillingUnit(),
                pricing.getPrice(),
                pricing.getMatchDurationMinutes(),
                pricing.getWarningBeforeExpiryMinutes(),
                pricing.getActive()
        );
    }
}
