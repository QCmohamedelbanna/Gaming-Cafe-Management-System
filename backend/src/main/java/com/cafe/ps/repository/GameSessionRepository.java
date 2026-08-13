package com.cafe.ps.repository;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findFirstByDeviceIdAndStatusOrderByStartTimeDesc(Long deviceId, SessionStatus status);
    List<GameSession> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);
    List<GameSession> findByStatus(SessionStatus status);
}
