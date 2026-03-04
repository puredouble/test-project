@Entity
@Table(name = "API_REQUEST_QUEUE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiRequestQueue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    
    // 낙관적 락을 위한 버전 필드 (JPA가 자동 관리함)
    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private ProviderName providerName;

    @Lob // Oracle의 경우 CLOB으로 매핑됨
    private String paramsJson;

    @Column(nullable = false)
    private String status; // WAIT, PROCESSING, SUCCESS, DEAD

    @Column(nullable = false)
    private int retryCount = 0;

    // 추가된 필드들
    @Column(length = 1000)
    private String lastErrorMessage; 

    @Column(nullable = false)
    private LocalDateTime nextRetryAt = LocalDateTime.now(); // 기본값: 즉시 실행

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // --- 상태 전이 메서드 ---
    public void markAsProcessing() { 
        this.status = "PROCESSING"; 
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsSuccess() { 
        this.status = "SUCCESS"; 
        this.updatedAt = LocalDateTime.now();
    }

    // 실패 처리 및 재시도 로직 (핵심)
    public void markAsFailed(String errorMessage) {
        this.lastErrorMessage = errorMessage != null && errorMessage.length() > 990 
                                ? errorMessage.substring(0, 990) : errorMessage;
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();

        if (this.retryCount >= 3) { // 최대 재시도 3회
            this.status = "DEAD";
        } else {
            this.status = "WAIT";
            // 점진적 재시도 지연 (1회 실패: 1분 뒤, 2회 실패: 2분 뒤 ...)
            this.nextRetryAt = LocalDateTime.now().plusMinutes(this.retryCount);
        }
    }

    // 좀비 복구용 메서드
    public void recoverToWait() {
        this.status = "WAIT";
        this.updatedAt = LocalDateTime.now();
    }
}