@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 연결 시도 시간 제한 (예: 3초)
                .setConnectTimeout(Duration.ofSeconds(3))
                // 데이터 읽기 대기 시간 제한 (예: 5초) - GET 요청의 최대 생존 시간
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}