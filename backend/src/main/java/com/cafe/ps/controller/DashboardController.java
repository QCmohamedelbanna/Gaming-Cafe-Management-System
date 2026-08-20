package com.cafe.ps.controller;
import com.cafe.ps.dto.DashboardSummary;
import com.cafe.ps.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController @RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERMISSION_DASHBOARD_VIEW')")
public class DashboardController {
    private final ReportService reportService;

    @GetMapping("/today")
    public DashboardSummary today() {
        return reportService.dashboardSummary();
    }
}
