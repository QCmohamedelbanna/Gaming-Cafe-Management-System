package com.cafe.ps.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    /**
     * Kept for databases created before pricing became independent from a
     * device. New devices use the default only to satisfy the legacy schema;
     * session prices come from the Pricing entity.
     */
    @JsonIgnore
    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal legacyHourlyRate = BigDecimal.ZERO;

    /**
     * Administrative availability switch. A null value is treated as active
     * for devices created before this field was introduced.
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Soft deletion marker. Deleted devices stay available to historical
     * sessions and billing records but are hidden from current operations.
     */
    @JsonIgnore
    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(length = 500)
    private String maintenanceNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_provider", nullable = false, length = 32)
    @Builder.Default
    private DeviceControlProvider controlProvider = DeviceControlProvider.NONE;

    /** Provider-owned device identifier. It is not a credential. */
    @Column(name = "controller_device_id", length = 255)
    private String controllerDeviceId;

    /** Provider-specific power instruction code discovered from the device. */
    @Column(name = "controller_power_code", length = 100)
    private String controllerPowerCode;

    @Column(name = "power_control_enabled", nullable = false)
    @Builder.Default
    private Boolean powerControlEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical_power_status", nullable = false, length = 32)
    @Builder.Default
    private DevicePowerState physicalPowerStatus = DevicePowerState.UNKNOWN;

    @Column(name = "last_control_at")
    private LocalDateTime lastControlAt;

    @Column(name = "last_control_error", length = 500)
    private String lastControlError;

    @Enumerated(EnumType.STRING)
    @Column(name = "shutdown_policy", nullable = false, length = 40)
    @Builder.Default
    private DeviceShutdownPolicy shutdownPolicy = DeviceShutdownPolicy.NONE;
}
