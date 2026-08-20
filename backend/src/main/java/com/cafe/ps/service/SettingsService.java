package com.cafe.ps.service;

import com.cafe.ps.dto.UpdateSettingsRequest;
import com.cafe.ps.entity.AppSettings;
import com.cafe.ps.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AppSettingsRepository repository;

    /** Lazily creates the single settings row with defaults on first use. */
    @Transactional
    public AppSettings get() {
        return repository.findAll().stream()
                .findFirst()
                .orElseGet(() -> repository.save(AppSettings.builder().build()));
    }

    @Transactional
    public AppSettings update(UpdateSettingsRequest request) {
        AppSettings settings = get();

        if (request.discountAllowedRoles().isEmpty()) {
            throw new IllegalArgumentException("At least one role must be allowed to apply discounts");
        }

        settings.setPreventNegativeStock(request.preventNegativeStock());
        settings.setDiscountAllowedRoles(new HashSet<>(request.discountAllowedRoles()));
        settings.setDashboardEndingSoonMinutes(request.dashboardEndingSoonMinutes());
        settings.setReservationsNoShowGraceMinutes(request.reservationsNoShowGraceMinutes());
        return repository.save(settings);
    }
}
