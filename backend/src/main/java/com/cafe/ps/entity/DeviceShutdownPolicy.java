package com.cafe.ps.entity;

/**
 * DIRECT_POWER is suitable only for the current low-risk PoC load (a phone
 * charger). It must not be used as the normal PlayStation shutdown method
 * until a safe console shutdown/rest-mode mechanism exists.
 */
public enum DeviceShutdownPolicy {
    NONE,
    DIRECT_POWER,
    SAFE_SHUTDOWN_THEN_POWER
}
