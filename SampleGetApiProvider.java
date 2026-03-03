package com.project.integration.api.service.provider.impl;

import com.project.integration.api.enums.ProviderName;
import com.project.integration.api.service.provider.AbstractApiProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class SampleGetApiProvider extends AbstractApiProvider {

    public SampleGetApiProvider(RestTemplate restTemplate) {
        super(restTemplate); // 부모 클래스에 RestTemplate 전달
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.SAMPLE_GET_API;
    }

    @Override
    protected Map<String, Object> fetch(Map<String, Object> params) throws Exception {
        // GET 요청용 URI 빌더 (파라미터 매핑)
        URI uri = UriComponentsBuilder.fromHttpUrl("https://api.external.com/v1/data")
                .queryParam("userId", params.get("userId"))
                .build()
                .toUri();

        // 외부 API 동기 호출 (GET)
        return restTemplate.getForObject(uri, Map.class);
    }
}