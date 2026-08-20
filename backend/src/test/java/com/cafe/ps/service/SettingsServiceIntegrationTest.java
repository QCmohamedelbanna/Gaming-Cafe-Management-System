package com.cafe.ps.service;

import com.cafe.ps.AbstractMySQLIntegrationTest;
import com.cafe.ps.dto.UpdateSettingsRequest;
import com.cafe.ps.entity.AppSettings;
import com.cafe.ps.entity.Role;
import com.cafe.ps.repository.AppSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false"
        }
)
class SettingsServiceIntegrationTest extends AbstractMySQLIntegrationTest {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private AppSettingsRepository appSettingsRepository;

    @BeforeEach
    void cleanDatabase() {
        appSettingsRepository.deleteAll();
    }

    @Test
    void firstReadLazilyCreatesTheSingleRowWithDefaults() {
        AppSettings settings = settingsService.get();

        assertThat(settings.getPreventNegativeStock()).isTrue();
        assertThat(settings.getDiscountAllowedRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.MANAGER);
        assertThat(settings.getDashboardEndingSoonMinutes()).isEqualTo(30);
        assertThat(settings.getReservationsNoShowGraceMinutes()).isEqualTo(20);
        assertThat(appSettingsRepository.count()).isEqualTo(1);
    }

    @Test
    void secondReadReusesTheSameRowRatherThanCreatingAnother() {
        AppSettings first = settingsService.get();
        AppSettings second = settingsService.get();

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(appSettingsRepository.count()).isEqualTo(1);
    }

    @Test
    void updatePersistsAndIsReflectedOnTheNextRead() {
        settingsService.update(new UpdateSettingsRequest(
                false, Set.of(Role.ADMIN), 45, 10
        ));

        AppSettings reloaded = settingsService.get();
        assertThat(reloaded.getPreventNegativeStock()).isFalse();
        assertThat(reloaded.getDiscountAllowedRoles()).containsExactly(Role.ADMIN);
        assertThat(reloaded.getDashboardEndingSoonMinutes()).isEqualTo(45);
        assertThat(reloaded.getReservationsNoShowGraceMinutes()).isEqualTo(10);
    }

    @Test
    void updateWithNoAllowedDiscountRolesIsRejected() {
        assertThatThrownBy(() -> settingsService.update(new UpdateSettingsRequest(
                true, Set.of(), 30, 20
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one role");
    }
}
