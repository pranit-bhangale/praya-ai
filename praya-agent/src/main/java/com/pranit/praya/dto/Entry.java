package com.pranit.praya.dto;

import lombok.Builder;

@Builder
public record Entry(
        String degree,
        String institution,
        String period
) {
}