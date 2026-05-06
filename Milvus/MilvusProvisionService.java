import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.credential.CreateCredentialParam;
import io.milvus.param.control.CreateDatabaseParam;
import io.milvus.param.role.CreateRoleParam;
import io.milvus.param.role.GrantRoleParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilvusProvisionService {

    private final MilvusAccountInfoRepository repository;

    public MilvusProvisionService(MilvusAccountInfoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void provisionMilvusResources(ProvisionRequest req) {
        // 1. JPA를 통한 중복 확인
        boolean exists = repository.existsByClusterHostAndDatabaseNameAndUsername(
                req.getHost(), req.getNewDatabase(), req.getNewUser());
        
        if (exists) {
            throw new IllegalArgumentException("해당 계정 및 데이터베이스 매핑이 이미 존재합니다.");
        }

        // 2. 런타임 커넥션 동적 생성 (관리자 권한 필요)
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(req.getHost())
                .withPort(req.getPort())
                .withAuthorization(req.getAdminUser(), req.getAdminPassword())
                .build();

        MilvusServiceClient client = new MilvusServiceClient(connectParam);

        try {
            // 3. 데이터베이스 생성 (존재하지 않을 경우)
            // 주의: 최신 버전 밀버스 SDK에서는 이미 존재하는 DB 생성 시 에러가 날 수 있으므로 예외 처리가 필요할 수 있습니다.
            client.createDatabase(CreateDatabaseParam.newBuilder()
                    .withDatabaseName(req.getNewDatabase())
                    .build());

            // 4. 유저(Credential) 생성
            client.createCredential(CreateCredentialParam.newBuilder()
                    .withUsername(req.getNewUser())
                    .withPassword(req.getNewPassword())
                    .build());

            // 5. 역할(Role) 생성 및 부여
            String roleName = req.getRoleType() + "_" + req.getNewDatabase(); // DB별 격리를 위해 롤 이름 조합
            
            // 역할 생성
            client.createRole(CreateRoleParam.newBuilder()
                    .withRoleName(roleName)
                    .build());

            // 요구사항에 맞는 세부 권한(Privilege)을 역할에 부여
            grantPrivilegesBasedOnRole(client, roleName, req.getNewDatabase(), req.getRoleType());

            // 최종적으로 생성된 역할을 유저에게 할당
            client.grantRole(GrantRoleParam.newBuilder()
                    .withRoleName(roleName)
                    .withUsername(req.getNewUser())
                    .build());

            // 6. JPA에 이력 저장
            MilvusAccountInfo info = new MilvusAccountInfo(
                    req.getHost(), req.getPort(), req.getNewDatabase(), req.getNewUser(), req.getRoleType());
            repository.save(info);

        } catch (Exception e) {
            throw new RuntimeException("밀버스 리소스 프로비저닝 실패: " + e.getMessage(), e);
        } finally {
            // 커넥션 자원 반환
            client.close();
        }
    }/**
     * 모든 콜렉션(*)을 대상으로 상세 권한을 부여합니다.
     */
    private void grantPrivilegesBasedOnRole(MilvusServiceClient client, String roleName, String dbName, String roleType) {
        // 권한 그룹 정의
        String[] readPrivileges = {"Search", "Query", "DescribeCollection", "ShowCollections"};
        String[] writePrivileges = {"Insert", "Delete", "Upsert", "Flush"};
        String[] adminPrivileges = {"CreateCollection", "DropCollection", "CreateIndex", "DropIndex", "LoadCollection", "ReleaseCollection"};

        switch (roleType.toLowerCase()) {
            case "콜렉션리드온리":
            case "collectionreadonly":
            case "데이터베이스리드온리":
            case "databasereadonly":
                grant(client, roleName, dbName, readPrivileges);
                break;

            case "콜렉션리드라이트":
            case "collectionreadwrite":
            case "데이터베이스리드라이트":
            case "databasereadwrite":
                grant(client, roleName, dbName, readPrivileges);
                grant(client, roleName, dbName, writePrivileges);
                break;

            case "콜렉션어드민":
            case "collectionadmin":
            case "데이터베이스어드민":
            case "databaseadmin":
                grant(client, roleName, dbName, readPrivileges);
                grant(client, roleName, dbName, writePrivileges);
                grant(client, roleName, dbName, adminPrivileges);
                break;

            default:
                throw new IllegalArgumentException("지원하지 않는 권한 타입입니다: " + roleType);
        }
    }

    private void grant(MilvusServiceClient client, String roleName, String dbName, String[] privileges) {
        for (String privilege : privileges) {
            client.grantPrivilege(GrantPrivilegeParam.newBuilder()
                    .withRoleName(roleName)
                    .withObjectName("Collection") // 모든 콜렉션 대상
                    .withObject("*")              // 와일드카드 사용
                    .withPrivilege(privilege)
                    .withDatabaseName(dbName)
                    .build());
        }
    }

    /**
     * 생성된 리소스(DB, 유저, 역할)를 삭제하고 JPA 기록을 제거합니다.
     */
    @Transactional
    public void deleteMilvusResources(ProvisionRequest req) {
        // 1. JPA 기록 확인
        MilvusAccountInfo info = repository.findByClusterHostAndDatabaseNameAndUsername(
                req.getHost(), req.getNewDatabase(), req.getNewUser())
                .orElseThrow(() -> new IllegalArgumentException("삭제할 리소스 정보를 찾을 수 없습니다."));

        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(req.getHost())
                .withPort(req.getPort())
                .withAuthorization(req.getAdminUser(), req.getAdminPassword())
                .build();

        MilvusServiceClient client = new MilvusServiceClient(connectParam);

        try {
            // 역할 이름 규칙 (생성 시와 동일해야 함)
            String roleName = info.getGrantedRole() + "_" + req.getNewDatabase();

            // 2. 유저 삭제
            client.deleteCredential(DeleteCredentialParam.newBuilder()
                    .withUsername(req.getNewUser())
                    .build());

            // 3. 역할 삭제 (역할에 부여된 권한은 역할 삭제 시 함께 해제됨)
            client.dropRole(DropRoleParam.newBuilder()
                    .withRoleName(roleName)
                    .build());

            // 4. 데이터베이스 삭제
            client.dropDatabase(DropDatabaseParam.newBuilder()
                    .withDatabaseName(req.getNewDatabase())
                    .build());

            // 5. JPA 기록 삭제
            repository.delete(info);

        } catch (Exception e) {
            throw new RuntimeException("밀버스 리소스 삭제 실패: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }
}