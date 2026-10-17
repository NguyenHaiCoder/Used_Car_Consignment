package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from User u join fetch u.role where lower(u.email) = lower(:email)")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query("select u from User u join fetch u.role where u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);
}
