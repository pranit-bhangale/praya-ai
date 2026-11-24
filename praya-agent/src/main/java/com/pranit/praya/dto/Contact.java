package com.pranit.praya.dto;

import lombok.Builder;

@Builder
public record Contact(
        String name,
        String email,
        String portfolioUrl,
        String linkedinUrl
) {
}