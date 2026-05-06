import javax.persistence.*;

@Entity
@Table(name = "milvus_account_info")
public class MilvusAccountInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clusterHost;
    private int clusterPort;
    private String databaseName;
    private String username;
    private String grantedRole;

    // 기본 생성자, Getter, Setter
    public MilvusAccountInfo() {}

    public MilvusAccountInfo(String clusterHost, int clusterPort, String databaseName, String username, String grantedRole) {
        this.clusterHost = clusterHost;
        this.clusterPort = clusterPort;
        this.databaseName = databaseName;
        this.username = username;
        this.grantedRole = grantedRole;
    }
    
    // ... (Getter/Setter 생략)
}