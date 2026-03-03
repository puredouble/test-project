@Service
public class HcpCreateApiProvider extends AbstractApiProvider<HcpCreateRequestDto, HcpCreateResponseDto> {

    public HcpCreateApiProvider(RestTemplate restTemplate, Validator validator) {
        super(restTemplate, validator);
    }

    @Override
    public ProviderName getProviderName() {
        return ProviderName.HCP_CREATE_RUNTIME;
    }

    @Override
    public Class<HcpCreateRequestDto> getRequestType() {
        return HcpCreateRequestDto.class; // 타입 클래스 반환
    }

    @Override
    protected HcpCreateResponseDto fetch(HcpCreateRequestDto requestDto) throws Exception {
        // 엄격한 타입 기반으로 안전하게 파라미터 접근
        String url = "https://api.hcp.com/runtime";
        
        // POST 요청 (restTemplate이 JSON 변환 자동 수행)
        return restTemplate.postForObject(url, requestDto, HcpCreateResponseDto.class);
    }
}