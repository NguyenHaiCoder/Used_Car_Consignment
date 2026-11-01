package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.UserToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    @Query("""
            select t from UserToken t
            join fetch t.user u
            join fetch u.role
            order by t.createdAt desc
            """)
    List<UserToken> findAllWithUserOrderByCreatedAtDesc();

    Optional<UserToken> findByTokenAndRevokedAtIsNull(String token);

    List<UserToken> findByUserIdAndRevokedAtIsNull(Long userId);

    List<UserToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
