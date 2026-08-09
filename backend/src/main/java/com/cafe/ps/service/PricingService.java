package com.cafe.ps.service;

import com.cafe.ps.entity.DeviceType;
import com.cafe.ps.entity.Pricing;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.PricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRepository pricingRepository;

    public Pricing getPricing(
            DeviceType deviceType,
            SessionType sessionType
    ) {

        Pricing pricing = pricingRepository
                .findByDeviceTypeAndSessionType(
                        deviceType,
                        sessionType
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Pricing is not configured for "
                                        + deviceType
                                        + " / "
                                        + sessionType
                        )
                );

        if (!Boolean.TRUE.equals(pricing.getActive())) {
            throw new IllegalStateException(
                    "Pricing is disabled for "
                            + deviceType
                            + " / "
                            + sessionType
            );
        }

        return pricing;
    }

    public List<Pricing> getAll() {
        return pricingRepository.findAll();
    }

    @Transactional
    public Pricing updatePrice(
            Long id,
            BigDecimal price,
            Integer matchDurationMinutes,
            Integer warningBeforeExpiryMinutes
    ) {

        Pricing pricing = pricingRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pricing rule not found"
                        )
                );

        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Price must be greater than zero"
                );
            }

            pricing.setPrice(price);
        }

        if (pricing.getSessionType() == SessionType.MATCH) {

            if (matchDurationMinutes != null) {
                if (matchDurationMinutes < 1) {
                    throw new IllegalArgumentException(
                            "Match duration must be greater than zero"
                    );
                }

                pricing.setMatchDurationMinutes(
                        matchDurationMinutes
                );
            }

            if (warningBeforeExpiryMinutes != null) {

                if (warningBeforeExpiryMinutes < 0) {
                    throw new IllegalArgumentException(
                            "Warning duration cannot be negative"
                    );
                }

                if (pricing.getMatchDurationMinutes() != null
                        && warningBeforeExpiryMinutes
                        >= pricing.getMatchDurationMinutes()) {

                    throw new IllegalArgumentException(
                            "Warning duration must be less than match duration"
                    );
                }

                pricing.setWarningBeforeExpiryMinutes(
                        warningBeforeExpiryMinutes
                );
            }
        }

        return pricingRepository.save(pricing);
    }
}