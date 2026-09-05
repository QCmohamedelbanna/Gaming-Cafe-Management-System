package com.cafe.ps.service;

import com.cafe.ps.config.TuyaProperties;
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

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "device-control.mode", havingValue = "tuya")
public class TuyaDeviceControlService implements DeviceControlService {

    private final TuyaCloudClient client;
    private final TuyaProperties properties;

    @Override
    public PowerCommandResult powerOn(Device device) {
        return setPower(device, true);
    }

    @Override
    public PowerCommandResult powerOff(Device device) {
        return setPower(device, false);
    }

    @Override
    public PowerCommandResult getPowerState(Device device) {
        if (!ready(device)) return notReady(device);

        try {
            return statusResult(device, client.getStatus(device.getControllerDeviceId()));
        } catch (TuyaCloudException exception) {
            return failure(exception);
        } catch (RuntimeException exception) {
            return PowerCommandResult.failure(
                    DeviceControlProvider.TUYA,
                    DevicePowerState.ERROR,
                    "Tuya status query failed"
            );
        }
    }

    @Override
    public DeviceControlDiagnosticsResponse getDiagnostics(Device device) {
        if (!readyForDiagnostics(device)) {
            PowerCommandResult result = notReady(device);
            return diagnostics(device, result, List.of());
        }

        try {
            TuyaDeviceFunctions functions = client.getFunctions(device.getControllerDeviceId());
            PowerCommandResult status = device.getControllerPowerCode() == null
                    ? PowerCommandResult.failure(
                    DeviceControlProvider.TUYA,
                    DevicePowerState.UNKNOWN,
                    "Power command code is not configured yet"
            )
                    : getPowerState(device);
            return diagnostics(device, status, functions.codes());
        } catch (TuyaCloudException exception) {
            PowerCommandResult result = failure(exception);
            return diagnostics(device, result, List.of());
        } catch (RuntimeException exception) {
            PowerCommandResult result = PowerCommandResult.failure(
                    DeviceControlProvider.TUYA,
                    DevicePowerState.ERROR,
                    "Tuya device diagnostics failed"
            );
            return diagnostics(device, result, List.of());
        }
    }

    private PowerCommandResult setPower(Device device, boolean desiredOn) {
        if (!ready(device)) return notReady(device);

        PowerCommandResult current = getPowerState(device);
        DevicePowerState desired = desiredOn ? DevicePowerState.ON : DevicePowerState.OFF;
        if (current.success() && current.physicalState() == desired) {
            return PowerCommandResult.success(
                    DeviceControlProvider.TUYA,
                    desired,
                    "Tuya device already " + desired
            );
        }

        try {
            client.sendCommand(
                    device.getControllerDeviceId(),
                    device.getControllerPowerCode(),
                    desiredOn
            );

            PowerCommandResult confirmed = getPowerState(device);
            if (confirmed.success() && confirmed.physicalState() == desired) {
                return PowerCommandResult.success(
                        DeviceControlProvider.TUYA,
                        desired,
                        "Tuya device powered " + desired
                );
            }

            String confirmationMessage = confirmed.message() == null
                    ? "state confirmation was unavailable"
                    : confirmed.message();
            return PowerCommandResult.failure(
                    DeviceControlProvider.TUYA,
                    confirmed.physicalState(),
                    "Tuya command was accepted but " + confirmationMessage
            );
        } catch (TuyaCloudException exception) {
            return failure(exception);
        } catch (RuntimeException exception) {
            return PowerCommandResult.failure(
                    DeviceControlProvider.TUYA,
                    DevicePowerState.ERROR,
                    "Tuya power command failed"
            );
        }
    }

    private PowerCommandResult statusResult(Device device, List<TuyaStatusEntry> statuses) {
        String code = device.getControllerPowerCode();
        for (TuyaStatusEntry status : statuses) {
            if (!code.equals(status.code())) continue;
            Boolean value = booleanValue(status.value());
            if (value == null) {
                return PowerCommandResult.failure(
                        DeviceControlProvider.TUYA,
                        DevicePowerState.UNKNOWN,
                        "Tuya power status code returned a non-boolean value"
                );
            }
            return PowerCommandResult.success(
                    DeviceControlProvider.TUYA,
                    value ? DevicePowerState.ON : DevicePowerState.OFF,
                    "Tuya power state queried"
            );
        }
        return PowerCommandResult.failure(
                DeviceControlProvider.TUYA,
                DevicePowerState.UNKNOWN,
                "Configured Tuya power command code was not present in device status"
        );
    }

    private DeviceControlDiagnosticsResponse diagnostics(
            Device device,
            PowerCommandResult result,
            List<String> codes
    ) {
        return new DeviceControlDiagnosticsResponse(
                device.getId(),
                device.getName(),
                DeviceControlProvider.TUYA,
                mask(device.getControllerDeviceId()),
                result.physicalState(),
                codes,
                result.success(),
                result.message(),
                result.timestamp()
        );
    }

    private PowerCommandResult failure(TuyaCloudException exception) {
        DevicePowerState state = exception.isConnectivityFailure()
                ? DevicePowerState.OFFLINE
                : DevicePowerState.ERROR;
        return PowerCommandResult.failure(
                DeviceControlProvider.TUYA,
                state,
                exception.getMessage()
        );
    }

    private PowerCommandResult notReady(Device device) {
        String message = !properties.isEnabled()
                ? "Tuya Cloud integration is disabled"
                : "Tuya device control is not configured for this device";
        return PowerCommandResult.failure(
                DeviceControlProvider.TUYA,
                DevicePowerState.UNKNOWN,
                message
        );
    }

    private boolean ready(Device device) {
        return readyForDiagnostics(device)
                && device.getControlProvider() == DeviceControlProvider.TUYA
                && Boolean.TRUE.equals(device.getPowerControlEnabled())
                && device.getControllerPowerCode() != null
                && !device.getControllerPowerCode().isBlank();
    }

    private boolean readyForDiagnostics(Device device) {
        return properties.isEnabled()
                && device.getControlProvider() == DeviceControlProvider.TUYA
                && device.getControllerDeviceId() != null
                && !device.getControllerDeviceId().isBlank();
    }

    private static Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof String string) {
            if ("true".equalsIgnoreCase(string)) return true;
            if ("false".equalsIgnoreCase(string)) return false;
        }
        return null;
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() <= 8) return "***";
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
