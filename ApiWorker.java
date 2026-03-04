@Component
@RequiredArgsConstructor
@Slf4j
public class ApiWorker {

    private final ApiQueueRepository queueRepository;
    private final Map<String, ApiProvider> providerMap;
    private final ObjectMapper objectMapper;

    // 1. 메인 큐 처리 스케줄러 (1초마다 실행)
    @Scheduled(fixedDelay = 1000) 
    public void processDbQueue() {ApiRequestQueue request = null;
        try {
            // 1. 대기열에서 꺼내어 선점 시도
            request = fetchAndLockNextRequest();
            if (request == null) return; // 큐가 비어있음

            // 2. API 실행
            ApiProvider provider = providerMap.get(request.getProviderName().getBeanName());
            Class<?> requestType = provider.getRequestType();
            Object requestDto = objectMapper.readValue(request.getParamsJson(), requestType);
            
            ApiResponseDto<?> result = ((ApiProvider<Object, ?>) provider).execute(requestDto);
            
            // 3. 결과 저장
            handleResult(request, result);

        } catch (ObjectOptimisticLockingFailureException e) {
            // [핵심] 다른 워커가 먼저 이 데이터를 가져갔음!
            // 에러가 아니라 자연스러운 동시성 처리 현상이므로 무시하고 넘어갑니다.
            log.debug("다른 워커가 이미 선점했습니다. 다음 스케줄에 재시도합니다.");
        } catch (Exception e) {
            if (request != null) {
                handleFailure(request, e.getMessage());
            }
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
        // PageRequest를 통해 LIMIT 1 구현 (DB 호환)
        List<ApiRequestQueue> requests = queueRepository.findNextAvailableRequests(
                LocalDateTime.now(), PageRequest.of(0, 1));
        
        if (requests.isEmpty()) return null;

        ApiRequestQueue req = requests.get(0);
        req.markAsProcessing(); 
        
        // saveAndFlush를 호출하여 트랜잭션 커밋 전에 UPDATE 쿼리를 날려 낙관적 락 경합을 유도함
        return queueRepository.saveAndFlush(req); 
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleResult(ApiRequestQueue request, ApiResponseDto<?> result) {
        if (result.isSuccess()) {
            request.markAsSuccess();
        } else {
            request.markAsFailed(result.getErrorMessage());
        }
        queueRepository.save(request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handleFailure(ApiRequestQueue request, String errorMessage) {
        request.markAsFailed("System Error: " + errorMessage);
        queueRepository.save(request);
    }
}