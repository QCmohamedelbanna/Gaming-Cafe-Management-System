package com.cafe.ps.controller;
import com.cafe.ps.dto.DashboardSummary;
import com.cafe.ps.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor @CrossOrigin(origins = "http://localhost:5173")
public class DashboardController {
    private final SessionService service;
    @GetMapping("/today") public DashboardSummary today() { return service.todaySummary(); }
}
