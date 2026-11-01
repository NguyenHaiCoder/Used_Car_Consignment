package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("""
            select rp from RolePermission rp
            join fetch rp.role
            join fetch rp.permission
            """)
    List<RolePermission> findAllFetched();

    List<RolePermission> findByRoleId(Long roleId);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RolePermission rp where rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
