package com.cafe.ps.service;

import com.cafe.ps.dto.DeviceControlDiagnosticsResponse;
import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "device-control.mode", havingValue = "none")
public class NoOpDeviceControlService implements DeviceControlService {
    @Override
    public PowerCommandResult powerOn(Device device) {
        return disabled();
    }

    @Override
    public PowerCommandResult powerOff(Device device) {
        return disabled();
    }

    @Override
    public PowerCommandResult getPowerState(Device device) {
        return disabled();
    }

    @Override
    public DeviceControlDiagnosticsResponse getDiagnostics(Device device) {
        PowerCommandResult result = disabled();
        return new DeviceControlDiagnosticsResponse(
                device.getId(), device.getName(), DeviceControlProvider.NONE, null,
                result.physicalState(), List.of(), result.success(), result.message(), result.timestamp()
        );
    }

    private static PowerCommandResult disabled() {
        return PowerCommandResult.failure(
                DeviceControlProvider.NONE,
                DevicePowerState.UNKNOWN,
                "Device control mode is disabled"
        );
    }
}
