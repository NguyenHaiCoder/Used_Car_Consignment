package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.auth.Permission;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.RolePermission;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.repository.PermissionRepository;
import fu.he.training.fall2026.repository.RolePermissionRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.PermissionGuard;
import fu.he.training.fall2026.security.Perms;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguyễn Việt Hải: đồng bộ enum {@link Perm} xuống bảng permissions mỗi lần khởi động.
 * <ul>
 *   <li>Chức năng mới: thêm vào bảng và bật sẵn cho role sở hữu.</li>
 *   <li>Chức năng đã có: cập nhật tên, mô tả, nhóm. Giữ nguyên bật/tắt Admin đã chỉnh.</li>
 *   <li>Mã không còn trong enum (gồm 4 quyền chung cũ): xóa grant rồi xóa chức năng.</li>
 *   <li>Grant cấp chéo sang role khác: xóa. Chức năng khóa: luôn bật.</li>
 * </ul>
 * Chạy sau {@link AuthDataLoader} (Order 10) vì cần bảng roles đã có đủ.
 */
@Slf4j
@Component
@Order(11)
@RequiredArgsConstructor
public class PermissionCatalogSync implements ApplicationRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionGuard permissionGuard;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<RoleCode, Role> roles = new EnumMap<>(RoleCode.class);
        roleRepository.findAll().forEach(r -> roles.put(r.getCode(), r));

        Map<String, Permission> existing = new HashMap<>();
        permissionRepository.findAll().forEach(p -> existing.put(p.getCode(), p));

        int removed = 0;
        for (Permission stale : existing.values()) {
            if (Perms.byCode(stale.getCode()) == null) {
                rolePermissionRepository.deleteByPermissionId(stale.getId());
                permissionRepository.delete(stale);
                removed++;
            }
        }

        Set<String> granted = new HashSet<>();
        int crossRole = 0;
        for (RolePermission rp : rolePermissionRepository.findAllFetched()) {
            Perm perm = Perms.byCode(rp.getPermission().getCode());
            if (perm != null && perm.role() != rp.getRole().getCode()) {
                rolePermissionRepository.delete(rp);
                crossRole++;
            } else if (perm != null) {
                granted.add(perm.name());
            }
        }

        int added = 0;
        Perm[] catalog = Perm.values();
        for (int i = 0; i < catalog.length; i++) {
            Perm perm = catalog[i];
            Permission row = existing.get(perm.name());
            boolean isNew = row == null;
            if (isNew) {
                row = new Permission();
                row.setCode(perm.name());
            }
            if (isNew || !sameMeta(row, perm, i)) {
                row.setName(perm.label());
                row.setDescription(perm.description());
                row.setModuleName(perm.module());
                row.setOwnerRole(perm.role());
                row.setSortOrder(i);
                row = permissionRepository.save(row);
            }
            Role owner = roles.get(perm.role());
            if (owner != null && (isNew || perm.locked()) && !granted.contains(perm.name())) {
                RolePermission rp = new RolePermission();
                rp.setRole(owner);
                rp.setPermission(row);
                rolePermissionRepository.save(rp);
                granted.add(perm.name());
            }
            if (isNew) {
                added++;
            }
        }

        permissionGuard.invalidate();
        if (added + removed + crossRole > 0) {
            log.info("[RBAC] Danh mục quyền: thêm {}, bỏ {}, gỡ {} grant cấp chéo. Tổng {} chức năng.",
                    added, removed, crossRole, catalog.length);
        }
    }

    private static boolean sameMeta(Permission row, Perm perm, int order) {
        return Objects.equals(row.getName(), perm.label())
                && Objects.equals(row.getDescription(), perm.description())
                && Objects.equals(row.getModuleName(), perm.module())
                && row.getOwnerRole() == perm.role()
                && Objects.equals(row.getSortOrder(), order);
    }
}
