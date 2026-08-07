package com.cafe.ps.controller;
import com.cafe.ps.dto.*;
import com.cafe.ps.entity.GameSession;
import com.cafe.ps.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/sessions") @RequiredArgsConstructor @CrossOrigin(origins = "http://localhost:5173")
public class SessionController {
    private final SessionService service;
    @PostMapping("/start") public GameSession start(@Valid @RequestBody StartSessionRequest req) { return service.start(req.deviceId(), req.sessionType(), req.plannedMinutes(), req.matchCount()); }
    @PostMapping("/{id}/stop") public GameSession stop(@PathVariable Long id) { return service.stop(id); }
    @PostMapping("/{id}/extend") public GameSession extend(@PathVariable Long id, @Valid @RequestBody ExtendSessionRequest req) { return service.extend(id, req.minutes()); }
    @PostMapping("/{id}/match/finish") public GameSession finishMatch(@PathVariable Long id) { return service.finishCurrentMatch(id); }
    @PostMapping("/{id}/match/add") public GameSession addMatch(@PathVariable Long id) { return service.addMatch(id); }
    @GetMapping("/active") public List<GameSession> active() { return service.activeSessions(); }
}
