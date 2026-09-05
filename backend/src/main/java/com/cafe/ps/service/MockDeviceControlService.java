package com.cafe.ps.service;

import com.cafe.ps.config.DeviceControlProperties;
import com.cafe.ps.dto.DeviceControlDiagnosticsResponse;
import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic provider for local development and automated tests. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "device-control.mode",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockDeviceControlService implements DeviceControlService {

    private final DeviceControlProperties properties;
    private final ConcurrentHashMap<Long, DevicePowerState> states = new ConcurrentHashMap<>();

    @Override
    public PowerCommandResult powerOn(Device device) {
        return command(device, DevicePowerState.ON);
    }

    @Override
    public PowerCommandResult powerOff(Device device) {
        return command(device, DevicePowerState.OFF);
    }

    @Override
    public PowerCommandResult getPowerState(Device device) {
        if (properties.isMockFailure()) {
            return PowerCommandResult.failure(
                    DeviceControlProvider.NONE,
                    DevicePowerState.OFFLINE,
                    properties.getMockFailureMessage()
            );
        }
        DevicePowerState state = states.getOrDefault(
                device.getId(),
                device.getPhysicalPowerStatus() == null
                        ? DevicePowerState.UNKNOWN
                        : device.getPhysicalPowerStatus()
        );
        return PowerCommandResult.success(DeviceControlProvider.NONE, state, "Mock power state");
    }

    @Override
    public DeviceControlDiagnosticsResponse getDiagnostics(Device device) {
        PowerCommandResult status = getPowerState(device);
        return new DeviceControlDiagnosticsResponse(
                device.getId(),
                device.getName(),
                DeviceControlProvider.NONE,
                null,
                status.physicalState(),
                List.of(),
                status.success(),
                status.message(),
                status.timestamp()
        );
    }

    private PowerCommandResult command(Device device, DevicePowerState desired) {
        if (properties.isMockFailure()) {
            return PowerCommandResult.failure(
                    DeviceControlProvider.NONE,
                    DevicePowerState.OFFLINE,
                    properties.getMockFailureMessage()
            );
        }
        DevicePowerState previous = states.put(device.getId(), desired);
        String message = previous == desired
                ? "Mock device already " + desired
                : "Mock device powered " + desired;
        return PowerCommandResult.success(DeviceControlProvider.NONE, desired, message);
    }
}
