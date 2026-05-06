import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/milvus")
public class MilvusController {

    private final MilvusProvisionService provisionService;

    public MilvusController(MilvusProvisionService provisionService) {
        this.provisionService = provisionService;
    }

    @PostMapping("/provision")
    public ResponseEntity<String> provisionMilvus(@RequestBody ProvisionRequest request) {
        try {
            provisionService.provisionMilvusResources(request);
            return ResponseEntity.ok("데이터베이스, 유저 생성 및 권한 부여가 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류: " + e.getMessage());
        }
    }
}