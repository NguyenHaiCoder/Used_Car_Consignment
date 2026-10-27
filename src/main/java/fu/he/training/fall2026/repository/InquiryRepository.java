package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query(value = """
            select i from Inquiry i
            left join fetch i.vehicle v
            left join fetch i.assignedTo u
            where i.buyer.id = :buyerId
            """,
            countQuery = "select count(i) from Inquiry i where i.buyer.id = :buyerId")
    Page<Inquiry> findByBuyerId(@Param("buyerId") Long buyerId, Pageable pageable);
}
