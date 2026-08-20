package com.cafe.ps.repository;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findFirstByDeviceIdAndStatusOrderByStartTimeDesc(Long deviceId, SessionStatus status);
    List<GameSession> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);
    List<GameSession> findByStatus(SessionStatus status);

    /**
     * Locks the session row for the duration of the checkout transaction so
     * two concurrent checkout attempts on the same session serialize instead
     * of both reading ACTIVE and racing to finalize/pay it twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameSession s where s.id = :id")
    Optional<GameSession> findByIdForUpdate(@Param("id") Long id);
}
