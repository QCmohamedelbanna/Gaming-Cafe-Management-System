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
     * Locks the session row for the duration of the owning transaction so
     * checkout, scheduled expiry, and manual finalization serialize before
     * deciding whether the session is still ACTIVE.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameSession s where s.id = :id")
    Optional<GameSession> findByIdForUpdate(@Param("id") Long id);

    /** Returns candidate ids without putting stale session entities in the persistence context. */
    @Query("select s.id from GameSession s where s.status = :status")
    List<Long> findIdsByStatus(@Param("status") SessionStatus status);
}
