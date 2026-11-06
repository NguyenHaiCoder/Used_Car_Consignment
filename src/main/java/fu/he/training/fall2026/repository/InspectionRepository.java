package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Inspection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    /** Rows of {consignment id, latest inspectedOn}; {@code consignmentIds} must not be empty. */
    @Query("""
            select i.consignment.id, max(i.inspectedOn) from Inspection i
            where i.consignment.id in :consignmentIds
            group by i.consignment.id
            """)
    List<Object[]> findLatestInspectedOn(@Param("consignmentIds") Collection<Long> consignmentIds);

    Optional<Inspection> findFirstByConsignmentIdOrderByIdDesc(Long consignmentId);
}
