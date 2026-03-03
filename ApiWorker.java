@Component
@RequiredArgsConstructor
@Slf4j
public class ApiWorker {

    private final ApiQueueRepository queueRepository;
    private final Map<String, ApiProvider> providerMap;
    private final ObjectMapper objectMapper;

    // 1. 메인 큐 처리 스케줄러 (1초마다 실행)
    @Scheduled(fixedDelay = 1000) 
    public void processDbQueue() {
        ApiRequestQueue request = fetchAndLockNextRequest();
        if (request == null) return; // 큐에 처리할 항목이 없음

        try {
            Map<String, Object> params = objectMapper.readValue(request.getParamsJson(), Map.class);
            ApiProvider provider = providerMap.get(request.getProviderName().getBeanName());
            
            // API 통신 실행
            ApiResponseDto result = provider.execute(params);
            handleResult(request, result);

        } catch (Exception e) {
            log.error("[API Failed] ID: {}, Error: {}", request.getId(), e.getMessage());
            handleFailure(request, e.getMessage()); // Exception 에러 메시지를 넘김
        }
    }

    // 2. 좀비 프로세스 복구 스케줄러 (5분마다 실행)
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void recoverZombieProcesses() {
        // 10분 이상 PROCESSING 상태로 멈춰있는 데이터 조회
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<ApiRequestQueue> zombies = queueRepository.findZombieRequests(threshold);
        
        for (ApiRequestQueue zombie : zombies) {
            log.warn("Recovering zombie process ID: {}", zombie.getId());
            zombie.recoverToWait(); // 다시 WAIT로 변경하여 재처리 유도
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected ApiRequestQueue fetchAndLockNextRequest() {
        return queueRepository.findNextAvailableRequest().map(req -> {
            req.markAsProcessing(); // 다른 워커가 가져가지 않도록 즉시 상태 변경
            return queueRepository.save(req);
        }).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleResult(ApiRequestQueue request, ApiResponseDto result) {
        if (result.isSuccess()) {
            request.markAsSuccess();
        } else {
            // API 응답은 정상이지만 논리적 에러(예: 파라미터 누락 등)인 경우
            request.markAsFailed(result.getErrorMessage());
        }
        queueRepository.save(request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleFailure(ApiRequestQueue request, String errorMessage) {
        // 네트워크 타임아웃, JSON 파싱 에러 등 시스템 예외인 경우
        request.markAsFailed("System Error: " + errorMessage);
        queueRepository.save(request);
    }
}