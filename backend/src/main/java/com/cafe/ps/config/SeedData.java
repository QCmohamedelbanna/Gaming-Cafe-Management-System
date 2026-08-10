package com.cafe.ps.config;
import com.cafe.ps.entity.*;
import com.cafe.ps.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
@Configuration
public class SeedData {
    @Bean CommandLineRunner seed(DeviceRepository devices, PricingRepository pricing, ProductRepository products) {
        return args -> {
            if (devices.count() == 0) {
                for (int i = 1; i <= 4; i++) devices.save(Device.builder().name("PS4-"+i).type(DeviceType.PS4).status(DeviceStatus.AVAILABLE).hourlyRate(new BigDecimal("40.00")).build());
                for (int i = 1; i <= 2; i++) devices.save(Device.builder().name("PS5-"+i).type(DeviceType.PS5).status(DeviceStatus.AVAILABLE).hourlyRate(new BigDecimal("60.00")).build());
            }
            seedPrice(
                    pricing,
                    DeviceType.PS4,
                    SessionType.SINGLE,
                    BillingUnit.HOUR,
                    "40.00",
                    null,
                    null
            );

            seedPrice(
                    pricing,
                    DeviceType.PS4,
                    SessionType.MULTI,
                    BillingUnit.HOUR,
                    "50.00",
                    null,
                    null
            );

            seedPrice(
                    pricing,
                    DeviceType.PS4,
                    SessionType.MATCH,
                    BillingUnit.MATCH,
                    "15.00",
                    15,
                    2
            );

            seedPrice(
                    pricing,
                    DeviceType.PS5,
                    SessionType.SINGLE,
                    BillingUnit.HOUR,
                    "60.00",
                    null,
                    null
            );

            seedPrice(
                    pricing,
                    DeviceType.PS5,
                    SessionType.MULTI,
                    BillingUnit.HOUR,
                    "80.00",
                    null,
                    null
            );

            seedPrice(
                    pricing,
                    DeviceType.PS5,
                    SessionType.MATCH,
                    BillingUnit.MATCH,
                    "25.00",
                    15,
                    2
            );

            seedProduct(products, "Tea", "15.00");
            seedProduct(products, "Coffee", "20.00");
            seedProduct(products, "Chips", "15.00");
            seedProduct(products, "Water", "10.00");
            seedProduct(products, "Pepsi", "20.00");
        };

    }
    private static void seedPrice(
            PricingRepository repo,
            DeviceType deviceType,
            SessionType sessionType,
            BillingUnit billingUnit,
            String price,
            Integer matchDurationMinutes,
            Integer warningBeforeExpiryMinutes
    ) {

        if (repo.findByDeviceTypeAndSessionType(
                deviceType,
                sessionType
        ).isEmpty()) {

            repo.save(
                    Pricing.builder()
                            .deviceType(deviceType)
                            .sessionType(sessionType)
                            .billingUnit(billingUnit)
                            .price(new BigDecimal(price))
                            .matchDurationMinutes(matchDurationMinutes)
                            .warningBeforeExpiryMinutes(
                                    warningBeforeExpiryMinutes
                            )
                            .active(true)
                            .build()
            );
        }
    }

    private static void seedProduct(
            ProductRepository repo,
            String name,
            String price
    ) {

        boolean exists = repo.findAll()
                .stream()
                .anyMatch(product ->
                        product.getName()
                                .equalsIgnoreCase(name)
                );

        if (!exists) {
            repo.save(
                    Product.builder()
                            .name(name)
                            .price(new BigDecimal(price))
                            .active(true)
                            .build()
            );
        }
    }
}
