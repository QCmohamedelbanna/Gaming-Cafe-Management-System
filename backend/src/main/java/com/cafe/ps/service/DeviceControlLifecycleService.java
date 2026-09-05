package com.cafe.ps.service;

import com.cafe.ps.audit.AuditLog;
import com.cafe.ps.dto.DeviceControlDiagnosticsResponse;
import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.dto.DevicePowerStatusResponse;
import com.cafe.ps.entity.Device;
import com.cafe.ps.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Function;

/**
 * Runs provider calls after the owning database transaction commits. This
 * keeps cloud latency outside session/billing locks and isolates failures from
 * the financial transaction.
 */
@Service
@RequiredArgsConstructor
public class DeviceControlLifecycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceControlLifecycleService.class);

    private final DeviceRepository deviceRepository;
    private final DeviceControlService deviceControlService;
    private final DeviceControlStateService stateService;

    public void powerOnAfterCommit(Long deviceId, String actor) {
        afterCommit(deviceId, actor, deviceControlService::powerOn, "DEVICE_POWER_ON");
    }

    public void powerOffAfterCommit(Long deviceId, String actor) {
        afterCommit(deviceId, actor, deviceControlService::powerOff, "DEVICE_POWER_OFF");
    }

    public PowerCommandResult powerOnNow(Long deviceId, String actor) {
        return execute(deviceId, actor, deviceControlService::powerOn, "DEVICE_POWER_ON");
    }

    public PowerCommandResult powerOffNow(Long deviceId, String actor) {
        return execute(deviceId, actor, deviceControlService::powerOff, "DEVICE_POWER_OFF");
    }

    public DevicePowerStatusResponse powerStateNow(Long deviceId, String actor) {
        Device device = requireDevice(deviceId);
        PowerCommandResult result;
        if (!hardwareEnabled(device)) {
            result = PowerCommandResult.failure(
                    com.cafe.ps.entity.DeviceControlProvider.NONE,
                    device.getPhysicalPowerStatus() == null
                            ? com.cafe.ps.entity.DevicePowerState.UNKNOWN
                            : device.getPhysicalPowerStatus(),
                    "Hardware control is not enabled for this device"
            );
            AuditLog.record("DEVICE_POWER_STATUS", actor, target(device), "SKIPPED: hardware control disabled");
        } else {
            try {
                result = deviceControlService.getPowerState(device);
            } catch (RuntimeException exception) {
                LOGGER.warn("Device power status failed for device {}: {}", deviceId, safe(exception.getMessage()));
                result = PowerCommandResult.failure(
                        device.getControlProvider(),
                        com.cafe.ps.entity.DevicePowerState.ERROR,
                        "Hardware status query failed"
                );
            }
            persistAndAudit(device, result, actor, "DEVICE_POWER_STATUS");
        }
        return new DevicePowerStatusResponse(
                device.getId(),
                device.getName(),
                result.provider(),
                result.physicalState(),
                result.success(),
                result.message(),
                result.timestamp()
        );
    }

    public DeviceControlDiagnosticsResponse diagnostics(Long deviceId, String actor) {
        Device device = requireDevice(deviceId);
        DeviceControlDiagnosticsResponse result;
        try {
            result = deviceControlService.getDiagnostics(device);
            if (hardwareEnabled(device)) {
                stateService.record(deviceId, new PowerCommandResult(
                        result.success(), result.provider(), result.physicalState(), result.message(), result.timestamp()
                ));
            }
            AuditLog.record(
                    "DEVICE_POWER_STATUS",
                    actor,
                    target(device),
                    outcome(result.success(), result.message())
            );
            if (!result.success()) {
                AuditLog.record("DEVICE_POWER_FAILED", actor, target(device), safe(result.message()));
            }
            return result;
        } catch (RuntimeException exception) {
            LOGGER.warn("Device power diagnostics failed for device {}: {}", deviceId, safe(exception.getMessage()));
            PowerCommandResult failure = PowerCommandResult.failure(
                    device.getControlProvider(),
                    com.cafe.ps.entity.DevicePowerState.ERROR,
                    "Hardware diagnostics failed"
            );
            recordFailure(device, failure, actor, "DEVICE_POWER_STATUS");
            return new DeviceControlDiagnosticsResponse(
                    device.getId(), device.getName(), device.getControlProvider(),
                    mask(device.getControllerDeviceId()), failure.physicalState(),
                    java.util.List.of(), false, failure.message(), failure.timestamp()
            );
        }
    }

    private void afterCommit(
            Long deviceId,
            String actor,
            Function<Device, PowerCommandResult> operation,
            String action
    ) {
        Runnable task = () -> execute(deviceId, actor, operation, action);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private PowerCommandResult execute(
            Long deviceId,
            String actor,
            Function<Device, PowerCommandResult> operation,
            String action
    ) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            LOGGER.warn("Hardware control skipped because device {} no longer exists", deviceId);
            return PowerCommandResult.failure(
                    com.cafe.ps.entity.DeviceControlProvider.NONE,
                    com.cafe.ps.entity.DevicePowerState.UNKNOWN,
                    "Device not found"
            );
        }

        if (!hardwareEnabled(device)) {
            PowerCommandResult skipped = PowerCommandResult.failure(
                    com.cafe.ps.entity.DeviceControlProvider.NONE,
                    device.getPhysicalPowerStatus() == null
                            ? com.cafe.ps.entity.DevicePowerState.UNKNOWN
                            : device.getPhysicalPowerStatus(),
                    "Hardware control is not enabled for this device"
            );
            AuditLog.record(action, actor, target(device), "SKIPPED: hardware control disabled");
            return skipped;
        }

        PowerCommandResult result;
        try {
            result = operation.apply(device);
        } catch (RuntimeException exception) {
            LOGGER.warn("Hardware control failed for device {}: {}", deviceId, safe(exception.getMessage()));
            result = PowerCommandResult.failure(
                    device.getControlProvider(),
                    com.cafe.ps.entity.DevicePowerState.ERROR,
                    "Hardware control failed"
            );
        }
        try {
            stateService.record(deviceId, result);
        } catch (RuntimeException exception) {
            // The command result is still audited; telemetry persistence must
            // never turn an already-committed session into a failed request.
            LOGGER.error("Could not persist hardware state for device {}", deviceId, exception);
        }
        AuditLog.record(action, actor, target(device), outcome(result.success(), result.message()));
        if (!result.success()) {
            AuditLog.record("DEVICE_POWER_FAILED", actor, target(device), safe(result.message()));
        }
        return result;
    }

    private static boolean hardwareEnabled(Device device) {
        return Boolean.TRUE.equals(device.getPowerControlEnabled())
                && device.getControlProvider() != null
                && device.getControlProvider() != com.cafe.ps.entity.DeviceControlProvider.NONE;
    }

    private void persistAndAudit(
            Device device,
            PowerCommandResult result,
            String actor,
            String action
    ) {
        try {
            stateService.record(device.getId(), result);
        } catch (RuntimeException exception) {
            LOGGER.error("Could not persist hardware state for device {}", device.getId(), exception);
        }
        AuditLog.record(action, actor, target(device), outcome(result.success(), result.message()));
        if (!result.success()) {
            AuditLog.record("DEVICE_POWER_FAILED", actor, target(device), safe(result.message()));
        }
    }

    private void recordFailure(Device device, PowerCommandResult result, String actor, String action) {
        try {
            stateService.record(device.getId(), result);
        } catch (RuntimeException exception) {
            LOGGER.error("Could not persist hardware failure for device {}", device.getId(), exception);
        }
        AuditLog.record(action, actor, target(device), outcome(false, result.message()));
    }

    private Device requireDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
    }

    private static String target(Device device) {
        return "device:" + device.getId()
                + ":" + device.getName()
                + " provider=" + (device.getControlProvider() == null ? "NONE" : device.getControlProvider())
                + " controller=" + mask(device.getControllerDeviceId());
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) return "none";
        if (value.length() <= 8) return "***";
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    private static String outcome(boolean success, String message) {
        return (success ? "SUCCESS" : "FAILURE") + (message == null ? "" : ": " + safe(message));
    }

    private static String safe(String message) {
        if (message == null || message.isBlank()) return "unknown";
        String sanitized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
    }
}
