package com.cafe.ps.repository;

import com.cafe.ps.entity.Reservation;
import com.cafe.ps.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByStatusOrderByStartTimeAsc(ReservationStatus status);

    List<Reservation> findByDeviceIdAndStatus(Long deviceId, ReservationStatus status);

    List<Reservation> findByStatusAndStartTimeBefore(ReservationStatus status, LocalDateTime threshold);
}
