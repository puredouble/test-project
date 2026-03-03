package com.project.integration.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponseDto<T> {
    private final String providerName;
    private final boolean success;
    private final T data; // 제네릭 타입 적용
    private final String errorMessage;
    private final long responseTimeMs;
    private final boolean isValidationError; // 유효성 검증 실패 여부 플래그
}