package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Buyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {
    @Override
    @Query("select b from Buyer b where b.id = :id and b.deletedAt is null")
    Optional<Buyer> findById(@Param("id") Long id);

    @Override
    @Query("select b from Buyer b where b.deletedAt is null")
    List<Buyer> findAll();

    @Override
    @Query("select b from Buyer b where b.deletedAt is null")
    List<Buyer> findAll(org.springframework.data.domain.Sort sort);

    @Override
    @Query("select b from Buyer b where b.deletedAt is null")
    Page<Buyer> findAll(Pageable pageable);

    @Override
    @Query("select b from Buyer b where b.deletedAt is null and b.id in :ids")
    List<Buyer> findAllById(@Param("ids") Iterable<Long> ids);

    @Override
    @Query("select count(b) from Buyer b where b.deletedAt is null")
    long count();

    @Override
    @Query("select count(b) > 0 from Buyer b where b.id = :id and b.deletedAt is null")
    boolean existsById(@Param("id") Long id);

    @Query("select count(b) > 0 from Buyer b where b.id = :id")
    boolean existsIncludingDeleted(@Param("id") Long id);

    @Query("select count(r) > 0 from Reservation r where r.buyer.id = :id and r.status = fu.he.training.fall2026.entity.enums.ReservationStatus.ACTIVE")
    boolean hasActiveReservation(@Param("id") Long id);

    @Query("""
            select count(c) > 0 from SalesContract c where c.reservation.buyer.id = :id
            and c.status not in (fu.he.training.fall2026.entity.enums.ContractStatus.CLOSED,
                                 fu.he.training.fall2026.entity.enums.ContractStatus.CANCELLED)
            """)
    boolean hasProcessingContract(@Param("id") Long id);

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


    @Query(value = "select b from Buyer b where b.deletedAt is null",
            countQuery = "select count(b) from Buyer b where b.deletedAt is null")
    Page<Buyer> findBuyerList(Pageable pageable);

 
    @Query(value = """
            select b from Buyer b where b.deletedAt is null
            and (lower(cast(b.id as String)) like :pattern escape '!'
               or lower(b.fullName) like :pattern escape '!'
               or lower(b.phone) like :pattern escape '!'
               or lower(b.email) like :pattern escape '!')
            """,
            countQuery = """
            select count(b) from Buyer b where b.deletedAt is null
            and (lower(cast(b.id as String)) like :pattern escape '!'
               or lower(b.fullName) like :pattern escape '!'
               or lower(b.phone) like :pattern escape '!'
               or lower(b.email) like :pattern escape '!')
            """)
    Page<Buyer> searchBuyerList(@Param("pattern") String pattern, Pageable pageable);
}
