package com.cafe.ps.dto;
import java.math.BigDecimal;
public record DashboardSummary(long totalDevices, long activeSessions, long completedSessionsToday, BigDecimal revenueToday) {}
