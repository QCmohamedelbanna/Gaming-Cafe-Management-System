package com.cafe.ps.controller;

import com.cafe.ps.service.DatabaseBackupService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Admin-only hook for a future backup button or maintenance screen. */
@RestController
@RequestMapping("/api/system")
public class DatabaseBackupController {

    private final DatabaseBackupService backupService;

    public DatabaseBackupController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping("/backup")
    @PreAuthorize("hasAuthority('PERMISSION_SETTINGS_MANAGE')")
    public Map<String, Object> backup() {
        DatabaseBackupService.BackupResult result = backupService.createBackup();
        return Map.of(
                "fileName", result.fileName(),
                "path", result.path().toString(),
                "sizeBytes", result.sizeBytes()
        );
    }
}
