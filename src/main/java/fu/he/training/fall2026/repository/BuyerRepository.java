package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Buyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
    Optional<Buyer> findByIdAndDeletedAtIsNull(Long id);

    Page<Buyer> findByDeletedAtIsNull(Pageable pageable);

    List<Buyer> findByDeletedAtIsNull(Sort sort);

    @Query(value = "select count_big(*) from buyers with (TABLOCKX, HOLDLOCK)", nativeQuery = true)
    long lockForCreate();

    @Query("select count(b) > 0 from Buyer b where trim(b.phone) = :phone")
    boolean contactPhoneExists(@Param("phone") String phone);

    @Query("select count(b) > 0 from Buyer b where lower(trim(b.email)) = :email")
    boolean contactEmailExists(@Param("email") String email);

    @Query("select count(b) > 0 from Buyer b where trim(b.phone) = :phone and b.id <> :id")
    boolean otherPhoneExists(@Param("phone") String phone, @Param("id") Long id);

    @Query("select count(b) > 0 from Buyer b where lower(trim(b.email)) = :email and b.id <> :id")
    boolean otherEmailExists(@Param("email") String email, @Param("id") Long id);


    @Query(value = """
            select b from Buyer b where
            (:status = 'all' or (:status = 'active' and b.deletedAt is null)
                or (:status = 'inactive' and b.deletedAt is not null))
            and (:emptySearch = true or lower(cast(b.id as String)) like :pattern escape '!'
               or lower(b.fullName) like :pattern escape '!'
               or lower(b.phone) like :pattern escape '!'
               or lower(b.email) like :pattern escape '!')
            """,
            countQuery = """
            select count(b) from Buyer b where
            (:status = 'all' or (:status = 'active' and b.deletedAt is null)
                or (:status = 'inactive' and b.deletedAt is not null))
            and (:emptySearch = true or lower(cast(b.id as String)) like :pattern escape '!'
               or lower(b.fullName) like :pattern escape '!'
               or lower(b.phone) like :pattern escape '!'
               or lower(b.email) like :pattern escape '!')
            """)
    Page<Buyer> searchBuyerList(@Param("pattern") String pattern, @Param("emptySearch") boolean emptySearch,
                                @Param("status") String status, Pageable pageable);
}
