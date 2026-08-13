package com.cafe.ps.repository;
import com.cafe.ps.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findAllByDeletedFalseOrDeletedIsNull();

    long countByDeletedFalseOrDeletedIsNull();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
