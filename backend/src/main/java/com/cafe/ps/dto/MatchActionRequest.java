package com.cafe.ps.dto;
import jakarta.validation.constraints.Min;
public record MatchActionRequest(@Min(1) Integer minutes) {}
