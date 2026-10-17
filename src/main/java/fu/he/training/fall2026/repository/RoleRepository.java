package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.enums.RoleCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleCode code);

    boolean existsByCode(RoleCode code);
}
