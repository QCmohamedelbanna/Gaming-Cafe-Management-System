package com.cafe.ps.service;

import com.cafe.ps.config.TuyaProperties;
import com.cafe.ps.dto.PowerCommandResult;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.DeviceControlProvider;
import com.cafe.ps.entity.DevicePowerState;
import com.cafe.ps.entity.DeviceStatus;
import com.cafe.ps.entity.DeviceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TuyaDeviceControlServiceTest {

    @Mock
    private TuyaCloudClient client;

    private TuyaDeviceControlService service;
    private Device device;

    @BeforeEach
    void setUp() {
        TuyaProperties properties = new TuyaProperties();
        properties.setEnabled(true);
        service = new TuyaDeviceControlService(client, properties);
        device = Device.builder()
                .id(7L)
                .name("POC-PLUG")
                .type(DeviceType.PS4)
                .status(DeviceStatus.AVAILABLE)
                .controlProvider(DeviceControlProvider.TUYA)
                .controllerDeviceId("tuya-device-7")
                .controllerPowerCode("relay_power")
                .powerControlEnabled(true)
                .build();
    }

    @Test
    void alreadyOnPowerOnIsIdempotentAndDoesNotSendAnotherCommand() {
        when(client.getStatus("tuya-device-7"))
                .thenReturn(List.of(new TuyaStatusEntry("relay_power", true)));

        PowerCommandResult result = service.powerOn(device);

        assertThat(result.success()).isTrue();
        assertThat(result.physicalState()).isEqualTo(DevicePowerState.ON);
        verify(client, never()).sendCommand(anyString(), anyString(), anyBoolean());
    }

    @Test
    void alreadyOffPowerOffIsIdempotentAndDoesNotSendAnotherCommand() {
        when(client.getStatus("tuya-device-7"))
                .thenReturn(List.of(new TuyaStatusEntry("relay_power", false)));

        PowerCommandResult result = service.powerOff(device);

        assertThat(result.success()).isTrue();
        assertThat(result.physicalState()).isEqualTo(DevicePowerState.OFF);
        verify(client, never()).sendCommand(anyString(), anyString(), anyBoolean());
    }

    @Test
    void cloudTimeoutIsMappedToOfflineWithoutLeakingProviderException() {
        when(client.getStatus("tuya-device-7"))
                .thenThrow(new TuyaCloudException("Tuya Cloud is unreachable or timed out", null, true, false));

        PowerCommandResult result = service.powerOff(device);

        assertThat(result.success()).isFalse();
        assertThat(result.physicalState()).isEqualTo(DevicePowerState.OFFLINE);
        assertThat(result.message()).contains("unreachable");
        verify(client).sendCommand("tuya-device-7", "relay_power", false);
    }
}
