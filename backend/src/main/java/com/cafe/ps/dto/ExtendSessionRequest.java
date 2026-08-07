package com.cafe.ps.dto;
import jakarta.validation.constraints.Min;
public record ExtendSessionRequest(@Min(1) int minutes) {}
