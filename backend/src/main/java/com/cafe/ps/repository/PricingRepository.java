package com.cafe.ps.repository;
import com.cafe.ps.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PricingRepository extends JpaRepository<Pricing, Long> {
    Optional<Pricing> findByDeviceTypeAndSessionType(DeviceType deviceType, SessionType sessionType);
}
