package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Consignor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsignorRepository extends JpaRepository<Consignor, Long> {

    /** {@code pattern} is a lower-cased LIKE pattern such as {@code %duc%}; use {@code %} to match all. */
    @Query("""
            select c from Consignor c
            where lower(c.fullName) like :pattern
               or c.phone like :pattern
               or c.idNumber like :pattern
               or lower(coalesce(c.email, '')) like :pattern
            """)
    Page<Consignor> search(@Param("pattern") String pattern, Pageable pageable);

    /** True when another consignor (id different from {@code excludeId}) has the same phone or CCCD. */
    @Query("""
            select count(c) > 0 from Consignor c
            where (c.phone = :phone or c.idNumber = :idNumber) and c.id <> :excludeId
            """)
    boolean existsDuplicate(
            @Param("phone") String phone,
            @Param("idNumber") String idNumber,
            @Param("excludeId") Long excludeId);
}
