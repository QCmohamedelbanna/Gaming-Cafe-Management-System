package com.cafe.ps.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes one line per sensitive action to the dedicated "AUDIT" logger
 * (see logback-spring.xml), which is routed to its own rotated JSON file
 * independent of the application's regular logs.
 */
public final class AuditLog {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private AuditLog() {
    }

    public static void record(String action, String actor, String target, String outcome) {
        AUDIT.info(
                "action={} actor={} target={} outcome={}",
                action,
                actor == null || actor.isBlank() ? "unknown" : actor,
                target,
                outcome
        );
    }
}
