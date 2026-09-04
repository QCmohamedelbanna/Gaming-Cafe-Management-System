package com.cafe.ps.service;

import com.cafe.ps.dto.CreateReservationRequest;
import com.cafe.ps.entity.Customer;
import com.cafe.ps.entity.Device;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.Reservation;
import com.cafe.ps.entity.ReservationStatus;
import com.cafe.ps.entity.SessionType;
import com.cafe.ps.repository.DeviceRepository;
import com.cafe.ps.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final DeviceRepository deviceRepository;
    private final CustomerService customerService;
    private final SessionService sessionService;
    private final SettingsService settingsService;

    @Value("${spring.task.scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Transactional(readOnly = true)
    public List<Reservation> upcoming() {
        return reservationRepository.findByStatusOrderByStartTimeAsc(ReservationStatus.UPCOMING);
    }

    @Transactional(readOnly = true)
    public List<Reservation> all() {
        return reservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reservation get(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }

    @Transactional
    public Reservation create(CreateReservationRequest request) {
        if (!request.startTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reservation start time must be in the future");
        }

        Device device = deviceRepository.findById(request.deviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));
        if (Boolean.TRUE.equals(device.getDeleted())) {
            throw new IllegalArgumentException("Device not found");
        }

        assertNoOverlap(device.getId(), request.startTime(), request.durationMinutes());

        Customer customer = customerService.findOrCreate(request.customerName(), request.customerPhone());

        return reservationRepository.save(Reservation.builder()
                .customer(customer)
                .device(device)
                .sessionType(request.sessionType())
                .startTime(request.startTime())
                .durationMinutes(request.durationMinutes())
                .status(ReservationStatus.UPCOMING)
                .notes(normalizeOptional(request.notes()))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public Reservation cancel(Long id, String reason) {
        Reservation reservation = get(id);
        if (reservation.getStatus() != ReservationStatus.UPCOMING) {
            throw new IllegalStateException("Only upcoming reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancelReason(normalizeOptional(reason));
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation checkIn(
            Long id,
            SessionType sessionType,
            Integer plannedMinutes,
            Integer matchCount
    ) {
        Reservation reservation = get(id);
        if (reservation.getStatus() != ReservationStatus.UPCOMING) {
            throw new IllegalStateException("Only upcoming reservations can be checked in");
        }

        GameSession session = sessionService.start(
                reservation.getDevice().getId(),
                sessionType,
                plannedMinutes,
                matchCount
        );

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setCheckedInAt(LocalDateTime.now());
        reservation.setGameSession(session);
        return reservationRepository.save(reservation);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void scheduledNoShowSweep() {
        if (!schedulingEnabled) return;
        markNoShows();
    }

    @Transactional
    public void markNoShows() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusMinutes(settingsService.get().getReservationsNoShowGraceMinutes());
        List<Reservation> overdue = reservationRepository.findByStatusAndStartTimeBefore(
                ReservationStatus.UPCOMING,
                threshold
        );

        overdue.forEach(reservation -> reservation.setStatus(ReservationStatus.NO_SHOW));
        if (!overdue.isEmpty()) reservationRepository.saveAll(overdue);
    }

    private void assertNoOverlap(
            Long deviceId,
            LocalDateTime startTime,
            Integer durationMinutes
    ) {
        LocalDateTime proposedEnd = durationMinutes == null
                ? null
                : startTime.plusMinutes(durationMinutes);

        boolean overlaps = reservationRepository
                .findByDeviceIdAndStatus(deviceId, ReservationStatus.UPCOMING)
                .stream()
                .anyMatch(existing -> {
                    LocalDateTime existingEnd = existing.getDurationMinutes() == null
                            ? null
                            : existing.getStartTime().plusMinutes(existing.getDurationMinutes());
                    return (existingEnd == null || startTime.isBefore(existingEnd))
                            && (proposedEnd == null || existing.getStartTime().isBefore(proposedEnd));
                });

        if (overlaps) {
            throw new IllegalStateException(
                    "This device already has a reservation that overlaps this time"
            );
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
