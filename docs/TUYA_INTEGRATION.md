# Tuya / Smart Life hardware-control PoC

This integration controls one Tuya/Smart Life Wi-Fi plug through Tuya Cloud.
The approved first load is a phone charger only. Do not connect a PS4/PS5 or
use `DIRECT_POWER` as a console-shutdown strategy until a safe shutdown/rest-
mode mechanism is implemented.

## Architecture and safety boundary

Session and billing services depend on the provider-neutral
`DeviceControlService`. The mock provider is the default for development and
tests. `TuyaDeviceControlService` is selected only when
`DEVICE_CONTROL_MODE=tuya`.

Session database work commits before the cloud operation starts:

* a committed session start schedules one ON operation;
* the one locked `ACTIVE -> COMPLETED` transition schedules one OFF operation;
* a Tuya timeout/offline/error records `ERROR`, `OFFLINE`, or `UNKNOWN` device
  telemetry and never rolls back the session, bill, payment, or inventory;
* software `DeviceStatus` and physical `physicalPowerStatus` are separate.

The existing pessimistic session/bill locks and unique bill-to-session
constraint remain responsible for exactly-once financial behavior.

## Tuya Developer Platform setup

Use Tuya's current Developer Platform/API Explorer as the source of truth for
region, available services, and device-specific functions.

1. Create or sign in to the [Tuya Developer Platform](https://developer.tuya.com/en/).
2. Create a Cloud Development project and select the data center matching the
   Smart Life account/device region.
3. Under Cloud > Cloud Services, subscribe/authorize the required IoT Core,
   Smart Home Basic Service, and device-management/control APIs.
4. Open the project Devices tab and choose Link Tuya App Account. Scan the QR
   code with the already-paired Smart Life app and complete authorization.
5. Record the project's Client ID and Client Secret, and the plug's Device ID.
   Never put the Client Secret in the database, frontend, documentation, or a
   committed file.
6. Use API Explorer to call the plug's device details, functions, and status.
   The application diagnostic endpoint also exposes only safe, parsed data:
   `GET /api/devices/{id}/power/diagnostics`.
7. Identify the boolean power instruction code returned by the device. Do not
   assume `switch_1`; it may be different for this plug.

Tuya documents the device APIs as:

* `GET /v1.0/devices/{device_id}/functions`
* `GET /v1.0/devices/{device_id}/status`
* `POST /v1.0/devices/{device_id}/commands`

The backend uses the current HMAC-SHA256 signing scheme, requests a token via
`GET /v1.0/token?grant_type=1`, caches it using the server-provided
`expire_time`, and refreshes it before expiration or after an authentication
failure. It never logs token/signature material.

## Backend configuration

Set these in the backend process environment or the untracked development
`.env` file:

```text
DEVICE_CONTROL_MODE=tuya
TUYA_ENABLED=true
TUYA_ENDPOINT=https://openapi.tuyaus.com
TUYA_CLIENT_ID=<project client id>
TUYA_CLIENT_SECRET=<project client secret>
TUYA_CONNECT_TIMEOUT=2s
TUYA_REQUEST_TIMEOUT=5s
TUYA_MAX_ATTEMPTS=2
```

Choose the endpoint for the project's actual data center. Development and
automated tests should use `DEVICE_CONTROL_MODE=mock` and
`TUYA_ENABLED=false`; no test contacts Tuya Cloud.

## Configure the application device

Create the normal Gaming Cafe device first. As an administrator, associate it
with the plug using the existing device-management permission:

```http
PATCH /api/devices/{applicationDeviceId}/control
Content-Type: application/json

{
  "provider": "TUYA",
  "controllerDeviceId": "<tuya-device-id>",
  "controllerPowerCode": "<boolean-code-from-functions>",
  "enabled": true,
  "shutdownPolicy": "DIRECT_POWER"
}
```

For discovery before the code is known, save the provider and device ID with
`enabled: false`, call the diagnostics endpoint, then save the discovered
code with `enabled: true`. `SAFE_SHUTDOWN_THEN_POWER` is deliberately rejected
until a real safe console-shutdown implementation exists.

Restricted manual operations are:

* `GET /api/devices/{id}/power` — device viewers can query status;
* `POST /api/devices/{id}/power/on` and `/off` — device managers can control;
* `GET /api/devices/{id}/power/diagnostics` — device managers can inspect
  supported command codes and status.

## Phone-charger validation procedure

Do not use a PS4 or PS5 for this PoC.

1. Plug only a phone charger into the Tuya plug and confirm it is ON in Smart Life.
2. In Gaming Cafe, query the configured device power state and confirm `ON`.
3. Use the restricted manual OFF operation; confirm the charger loses power.
4. Use manual ON; confirm the charger receives power again.
5. Start a test Gaming Cafe session; confirm the application schedules ON.
6. Stop/finalize the session; confirm billing completes normally and the plug
   is turned OFF.
7. Start a planned timed session and let it expire; confirm a pending bill is
   created, the plug turns OFF, and no inventory sale occurs until payment.
8. Disconnect the backend's internet access. Start/stop a test session and
   confirm session/billing still work while the device reports a hardware
   warning/error.
9. Restore internet access, refresh status, and retry manual control.

Before any higher-risk load, add and validate a safe device-specific shutdown
mechanism. Directly cutting console power can corrupt data or damage the
customer experience and is outside this PoC.
