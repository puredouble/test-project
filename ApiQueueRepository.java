public interface ApiQueueRepository extends JpaRepository<ApiRequestQueue, Long> {
        
    @Query("SELECT q FROM ApiRequestQueue q WHERE q.status = 'WAIT' AND q.nextRetryAt <= :now ORDER BY q.id ASC")
    List<ApiRequestQueue> findNextAvailableRequests(@Param("now") LocalDateTime now, Pageable pageable);

    // 좀비 프로세스(10분 이상 PROCESSING) 조회용
    @Query("SELECT q FROM ApiRequestQueue q WHERE q.status = 'PROCESSING' AND q.updatedAt <= :threshold")
    List<ApiRequestQueue> findZombieRequests(@Param("threshold") LocalDateTime threshold);
}