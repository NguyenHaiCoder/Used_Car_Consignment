package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Inquiry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query("""
            select i from Inquiry i
              join fetch i.vehicle v
              left join fetch i.assignedTo
            where i.id = :id
            """)
    Optional<Inquiry> findDetailById(@Param("id") Long id);
}
