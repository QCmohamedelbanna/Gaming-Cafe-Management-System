package com.cafe.ps.config;
import com.cafe.ps.entity.*;
import com.cafe.ps.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
@Configuration
public class SeedData {
    @Bean CommandLineRunner seed(DeviceRepository devices, PricingRepository pricing) {
        return args -> {
            if (devices.count() == 0) {
                for (int i = 1; i <= 4; i++) devices.save(Device.builder().name("PS4-"+i).type(DeviceType.PS4).status(DeviceStatus.AVAILABLE).hourlyRate(new BigDecimal("40.00")).build());
                for (int i = 1; i <= 2; i++) devices.save(Device.builder().name("PS5-"+i).type(DeviceType.PS5).status(DeviceStatus.AVAILABLE).hourlyRate(new BigDecimal("60.00")).build());
            }
            seedPrice(pricing, DeviceType.PS4, SessionType.SINGLE, BillingUnit.HOUR, "40.00", null);
            seedPrice(pricing, DeviceType.PS4, SessionType.MULTI, BillingUnit.HOUR, "50.00", null);
            seedPrice(pricing, DeviceType.PS4, SessionType.MATCH, BillingUnit.MATCH, "15.00", 15);
            seedPrice(pricing, DeviceType.PS5, SessionType.SINGLE, BillingUnit.HOUR, "60.00", null);
            seedPrice(pricing, DeviceType.PS5, SessionType.MULTI, BillingUnit.HOUR, "80.00", null);
            seedPrice(pricing, DeviceType.PS5, SessionType.MATCH, BillingUnit.MATCH, "25.00", 15);
        };
    }
    private static void seedPrice(PricingRepository repo, DeviceType d, SessionType s, BillingUnit b, String p, Integer minutes) {
        if (repo.findByDeviceTypeAndSessionType(d, s).isEmpty()) repo.save(Pricing.builder().deviceType(d).sessionType(s).billingUnit(b).price(new BigDecimal(p)).matchDurationMinutes(minutes).build());
    }
}
