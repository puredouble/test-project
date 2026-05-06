public class ProvisionRequest {
    private String host;          // 런타임 접속 밀버스 호스트
    private int port;             // 런타임 접속 밀버스 포트
    private String adminUser;     // 밀버스 관리자 계정 (root)
    private String adminPassword; // 밀버스 관리자 비밀번호
    
    private String newDatabase;   // 생성할 DB명
    private String newUser;       // 생성할 유저명
    private String newPassword;   // 생성할 유저 비밀번호
    private String roleType;      // 부여할 권한 (예: CollectionAdmin, DatabaseReadOnly 등)

    // ... (Getter/Setter 생략)
}