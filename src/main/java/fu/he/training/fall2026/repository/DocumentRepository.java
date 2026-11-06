package fu.he.training.fall2026.repository;
import fu.he.training.fall2026.entity.consignment.Document;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("""
        select d from Document d where d.checklist.consignment.id = :consignmentId
        and d.checklist.kind = fu.he.training.fall2026.entity.enums.ChecklistKind.INTAKE
        and not exists (select newer.id from Document newer
            where newer.checklist.consignment.id = :consignmentId
            and newer.checklist.kind = fu.he.training.fall2026.entity.enums.ChecklistKind.INTAKE
            and newer.documentType = d.documentType
            and (newer.versionNo > d.versionNo or (newer.versionNo = d.versionNo and newer.id > d.id)))
        """)
    List<Document> findLatestIntakeDocuments(Long consignmentId);
}
