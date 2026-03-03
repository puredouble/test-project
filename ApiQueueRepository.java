public interface ApiQueueRepository extends JpaRepository<ApiRequestQueue, Long> {
    
    // nextRetryAt이 현재 시간보다 이전인 WAIT 건만 가져옵니다. (지연 재시도 적용)
    // Oracle 전용 SKIP LOCKED 문법 사용 (동시성 극대화)
    @Query(value = "SELECT * FROM API_REQUEST_QUEUE " +
                   "WHERE status = 'WAIT' AND next_retry_at <= SYSDATE " +
                   "ORDER BY id ASC FETCH FIRST 1 ROWS ONLY FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    Optional<ApiRequestQueue> findNextAvailableRequest();

    // 좀비 프로세스(10분 이상 PROCESSING) 조회용
    @Query("SELECT q FROM ApiRequestQueue q WHERE q.status = 'PROCESSING' AND q.updatedAt <= :threshold")
    List<ApiRequestQueue> findZombieRequests(@Param("threshold") LocalDateTime threshold);
}