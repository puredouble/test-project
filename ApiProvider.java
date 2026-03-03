package com.project.integration.api.service.provider;

import com.project.integration.api.dto.ApiResponseDto;
import com.project.integration.api.enums.ProviderName;

public interface ApiProvider<REQ, RES> {

    ProviderName getProviderName();
    
    // 워커가 JSON을 이 클래스 타입으로 변환할 수 있도록 타입 정보 제공
    Class<REQ> getRequestType(); 
    
    ApiResponseDto<RES> execute(REQ requestDto);
    
}