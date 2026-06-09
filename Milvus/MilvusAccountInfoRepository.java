import org.springframework.data.jpa.repository.JpaRepository;

public interface MilvusAccountInfoRepository extends JpaRepository<MilvusAccountInfo, Long> {
    // 동일한 클러스터 내에 해당 유저명과 데이터베이스명이 이미 존재하는지 확인
    boolean existsByClusterHostAndDatabaseNameAndUsername(String clusterHost, String databaseName, String username);
}