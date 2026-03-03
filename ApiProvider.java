package com.project.integration.api.service.provider;

import com.project.integration.api.dto.ApiResponseDto;
import com.project.integration.api.enums.ProviderName;
import java.util.Map;

public interface ApiProvider {
    /**
     * API 서비스 식별자 반환
     */
    ProviderName getProviderName();

    /**
     * API 호출 실행 (큐 워커나 일반 서비스에서 공통으로 호출)
     */
    ApiResponseDto execute(Map<String, Object> params);
}