package com.project.integration.api.service.provider;

import com.project.integration.api.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiProvider<REQ, RES> implements ApiProvider<REQ, RES> {

    protected final RestTemplate restTemplate;
    protected final Validator validator; // 유효성 검증기 주입

    @Override
    public ApiResponseDto<RES> execute(REQ requestDto) {
        long start = System.currentTimeMillis();

        // 1. DTO 유효성 검증 (Validation)
        Set<ConstraintViolation<REQ>> violations = validator.validate(requestDto);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            
            log.warn("[{}] Validation Failed: {}", getProviderName().name(), errorMessage);
            return buildResponse(false, null, errorMessage, start, true);
        }

        // 2. 외부 API 호출
        try {
            RES result = fetch(requestDto);
            return buildResponse(true, result, null, start, false);
            
        } catch (Exception e) {
            log.error("[{}] API 연동 실패: {}", getProviderName().name(), e.getMessage());
            return buildResponse(false, null, e.getMessage(), start, false);
        }
    }

    protected abstract RES fetch(REQ requestDto) throws Exception;

    private ApiResponseDto<RES> buildResponse(boolean success, RES data, String error, long start, boolean isValidationErr) {
        return ApiResponseDto.<RES>builder()
                .providerName(getProviderName().name())
                .success(success)
                .data(data) 
                .errorMessage(error)
                .responseTimeMs(System.currentTimeMillis() - start)
                .isValidationError(isValidationErr)
                .build();
    }
}