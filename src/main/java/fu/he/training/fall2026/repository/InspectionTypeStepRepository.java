package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.InspectionTypeStep;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InspectionTypeStepRepository extends JpaRepository<InspectionTypeStep, Long> {

    List<InspectionTypeStep> findByInspectionTypeIdOrderByStepOrderAsc(Long inspectionTypeId);

    /** Rows of {inspection type id, step count}; {@code typeIds} must not be empty. */
    @Query("""
            select s.inspectionType.id, count(s) from InspectionTypeStep s
            where s.inspectionType.id in :typeIds
            group by s.inspectionType.id
            """)
    List<Object[]> countByInspectionTypeIds(@Param("typeIds") Collection<Long> typeIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from InspectionTypeStep s where s.inspectionType.id = :inspectionTypeId")
    void deleteByInspectionTypeId(@Param("inspectionTypeId") Long inspectionTypeId);
}
