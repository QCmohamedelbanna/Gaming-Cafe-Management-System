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
7. Identify the boolean power instruction code returned by the device. For the
   verified phone-charger plug in this project, the code is `switch_1`.

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
TUYA_ENDPOINT=https://openapi.tuyaeu.com
TUYA_CLIENT_ID=<project client id>
TUYA_CLIENT_SECRET=<project client secret>
TUYA_CONNECT_TIMEOUT=2s
TUYA_REQUEST_TIMEOUT=5s
TUYA_MAX_ATTEMPTS=2
```

This verified project uses the Central Europe endpoint above. Development and
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
  "controllerPowerCode": "switch_1",
  "enabled": true,
  "shutdownPolicy": "DIRECT_POWER"
}
```

For the verified physical test, create or edit a logical device named `PS4-1`.
The name and device type are application metadata only; the actual controlled
load must remain the phone charger:

```text
PS4-1
provider = TUYA
controllerDeviceId = <real Tuya device id>
controllerPowerCode = switch_1
enabled = true
shutdownPolicy = DIRECT_POWER
```

The Client ID and Client Secret belong only in the backend process environment.
They are never entered in this device form or stored on the `devices` row.

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

Use an authenticated Admin/manager session for the restricted operations below.
The exact API paths are:

```text
GET  /api/devices/{id}/power/diagnostics
GET  /api/devices/{id}/power
POST /api/devices/{id}/power/on
POST /api/devices/{id}/power/off
```

* **Test A — backend diagnostics:** Confirm the diagnostics response reports
  provider `TUYA`, the masked device ID, code `switch_1`, the code in the
  supported functions, a readable physical state, and success/online status.
* **Test B — manual ON:** Turn the plug OFF first, call `POST .../power/on`,
  and confirm the plug turns ON and the phone begins charging.
* **Test C — manual OFF:** Call `POST .../power/off` and confirm the plug turns
  OFF and the phone stops charging.
* **Test D — start session:** With the plug OFF, start a Gaming Cafe session
  for `PS4-1`. Confirm the session is `ACTIVE`, the logical device is
  `PLAYING`, and the plug turns ON.
* **Test E — manual stop:** Stop/finalize the active session. Confirm the
  session is `COMPLETED`, the bill is correct, and the plug turns OFF.
* **Test F — checkout:** Start another session and use checkout. Confirm one
  bill is created, payment behavior is correct, and exactly one OFF lifecycle
  operation is issued.
* **Test G — automatic expiry:** Start a short planned session and let the
  scheduler expire it. Confirm the session is `COMPLETED`, the bill is
  `PENDING_PAYMENT` when applicable, the plug turns OFF, and inventory remains
  unchanged until payment.
* **Test H — internet failure:** Start a session while connected, then block
  backend internet access before finalization. Confirm the session, bill,
  payment, and inventory state remain correct even though OFF reports a
  hardware warning and the device records `ERROR` or `OFFLINE`. Restore
  internet, refresh status, and verify manual control recovers.

Before any higher-risk load, add and validate a safe device-specific shutdown
mechanism. Directly cutting console power can corrupt data or damage the
customer experience and is outside this PoC.
