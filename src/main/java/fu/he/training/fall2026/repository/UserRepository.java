package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from User u join fetch u.role where lower(u.email) = lower(:email)")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query("select u from User u join fetch u.role where u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    @Query("select u from User u join fetch u.role order by u.id asc")
    List<User> findAllWithRole();

    long countByActiveTrue();

    /** Tài khoản không bị hồ sơ nghiệp vụ trỏ tới. Phiên, OTP và nhật ký không tính. */
    @Query(value = """
            SELECT u.id FROM users u
            WHERE NOT EXISTS (SELECT 1 FROM approvals a
                              WHERE a.requested_by_user_id = u.id OR a.approved_by_user_id = u.id)
              AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.uploaded_by_user_id = u.id)
              AND NOT EXISTS (SELECT 1 FROM handovers h WHERE h.handed_over_by_user_id = u.id)
              AND NOT EXISTS (SELECT 1 FROM inquiries i WHERE i.assigned_to_user_id = u.id)
              AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.assigned_to_user_id = u.id)
              AND NOT EXISTS (SELECT 1 FROM transfer_cases t WHERE t.responsible_user_id = u.id)
              AND NOT EXISTS (SELECT 1 FROM vehicle_sale_news n WHERE n.assigned_to_user_id = u.id)
              AND NOT EXISTS (SELECT 1 FROM viewings v WHERE v.assigned_to_user_id = u.id)
            """, nativeQuery = true)
    List<Number> findIdsWithoutBusinessLinks();

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM auth_tokens WHERE user_id = :id", nativeQuery = true)
    void deleteAuthTokensByUserId(@Param("id") Long id);
}
