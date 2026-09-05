package com.cafe.ps.service;

import com.cafe.ps.dto.DeviceControlDiagnosticsResponse;
import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.entity.Device;

/** Provider-neutral boundary between session operations and physical devices. */
public interface DeviceControlService {
    PowerCommandResult powerOn(Device device);

    PowerCommandResult powerOff(Device device);

    PowerCommandResult getPowerState(Device device);

    DeviceControlDiagnosticsResponse getDiagnostics(Device device);
}
