package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.admin.PermissionMatrix;
import fu.he.training.fall2026.dto.admin.SavePermissionsRequest;
import fu.he.training.fall2026.entity.auth.Permission;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.RolePermission;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.PermissionRepository;
import fu.he.training.fall2026.repository.RolePermissionRepository;
import fu.he.training.fall2026.repository.RoleRepository;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.PermissionGuard;
import fu.he.training.fall2026.security.Perms;
import fu.he.training.fall2026.service.AdminPermissionService;
import fu.he.training.fall2026.service.AuditService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguyễn Việt Hải: màn phân quyền. Danh mục lấy từ enum {@link Perm}, trạng thái bật/tắt lấy từ role_permissions.
 * Lưu chỉ thêm/xóa đúng những dòng thay đổi, rồi xóa cache để có hiệu lực ngay.
 */
@Service
@RequiredArgsConstructor
public class AdminPermissionServiceImpl implements AdminPermissionService {

    private static final int AUDIT_DETAIL_MAX = 1000;

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditService auditService;
    private final PermissionGuard permissionGuard;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionMatrix.RoleTab> matrix() {
        Map<RoleCode, String> roleNames = new EnumMap<>(RoleCode.class);
        roleRepository.findAll().forEach(r -> roleNames.put(r.getCode(), r.getName()));
        Set<Perm> enabled = currentGrants().keySet();

        List<PermissionMatrix.RoleTab> tabs = new ArrayList<>();
        for (RoleCode role : RoleCode.values()) {
            Map<String, List<PermissionMatrix.Item>> modules = new LinkedHashMap<>();
            int on = 0;
            int total = 0;
            for (Perm perm : Perm.values()) {
                if (perm.role() != role) {
                    continue;
                }
                boolean isOn = perm.locked() || enabled.contains(perm);
                modules.computeIfAbsent(perm.module(), k -> new ArrayList<>()).add(new PermissionMatrix.Item(
                        perm.name(), perm.label(), perm.description(), isOn, perm.locked(), grantKey(role, perm)));
                total++;
                if (isOn) {
                    on++;
                }
            }
            if (total == 0) {
                continue;
            }
            List<PermissionMatrix.ModuleGroup> groups = modules.entrySet().stream()
                    .map(e -> new PermissionMatrix.ModuleGroup(e.getKey(), List.copyOf(e.getValue())))
                    .toList();
            tabs.add(new PermissionMatrix.RoleTab(role, roleNames.getOrDefault(role, role.name()), on, total, groups));
        }
        return tabs;
    }

    @Override
    @Transactional
    public String saveGrants(SavePermissionsRequest request, User actor) {
        Set<Perm> wanted = EnumSet.noneOf(Perm.class);
        List<String> raw = request.getGrants() == null ? List.of() : request.getGrants();
        for (String key : raw) {
            wanted.add(parse(key));
        }
        for (Perm perm : Perm.values()) {
            if (perm.locked()) {
                wanted.add(perm);
            }
        }

        Map<Perm, RolePermission> current = currentGrants();
        Map<RoleCode, Role> roles = new EnumMap<>(RoleCode.class);
        roleRepository.findAll().forEach(r -> roles.put(r.getCode(), r));
        Map<String, Permission> rows = new HashMap<>();
        permissionRepository.findAll().forEach(p -> rows.put(p.getCode(), p));

        List<Perm> turnedOn = new ArrayList<>();
        List<Perm> turnedOff = new ArrayList<>();
        for (Perm perm : Perm.values()) {
            boolean isOn = current.containsKey(perm);
            if (wanted.contains(perm) && !isOn) {
                Role role = roles.get(perm.role());
                Permission row = rows.get(perm.name());
                if (role == null || row == null) {
                    throw new BusinessException("Danh mục quyền chưa đồng bộ, khởi động lại ứng dụng rồi thử lại.");
                }
                RolePermission grant = new RolePermission();
                grant.setRole(role);
                grant.setPermission(row);
                rolePermissionRepository.save(grant);
                turnedOn.add(perm);
            } else if (!wanted.contains(perm) && isOn) {
                rolePermissionRepository.delete(current.get(perm));
                turnedOff.add(perm);
            }
        }
        rolePermissionRepository.flush();
        permissionGuard.invalidate();

        if (turnedOn.isEmpty() && turnedOff.isEmpty()) {
            return "Không có thay đổi nào.";
        }
        auditService.record(actor, "SAVE_PERMISSIONS", "RolePermission", null, auditDetail(turnedOn, turnedOff));
        return "Đã lưu phân quyền: bật " + turnedOn.size() + ", tắt " + turnedOff.size()
                + " chức năng. Có hiệu lực ngay.";
    }

    /** Chức năng đang bật, chỉ tính grant đúng role sở hữu. */
    private Map<Perm, RolePermission> currentGrants() {
        Map<Perm, RolePermission> map = new EnumMap<>(Perm.class);
        for (RolePermission rp : rolePermissionRepository.findAllFetched()) {
            Perm perm = Perms.byCode(rp.getPermission().getCode());
            if (perm != null && perm.role() == rp.getRole().getCode()) {
                map.put(perm, rp);
            }
        }
        return map;
    }

    private static Perm parse(String key) {
        if (key == null || !key.contains("|")) {
            throw new BusinessException("Dữ liệu phân quyền không hợp lệ.");
        }
        String[] parts = key.split("\\|", 2);
        Perm perm = Perms.byCode(parts[1].trim().toUpperCase(Locale.ROOT));
        if (perm == null) {
            throw new BusinessException("Chức năng không tồn tại: " + parts[1].trim());
        }
        if (!perm.role().name().equals(parts[0].trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("Không thể cấp \"" + perm.label() + "\" cho vai trò khác ngoài vai trò sở hữu.");
        }
        return perm;
    }

    private static String auditDetail(List<Perm> on, List<Perm> off) {
        StringBuilder sb = new StringBuilder();
        if (!on.isEmpty()) {
            sb.append("Bật: ").append(describe(on));
        }
        if (!off.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append("Tắt: ").append(describe(off));
        }
        return sb.length() > AUDIT_DETAIL_MAX ? sb.substring(0, AUDIT_DETAIL_MAX - 1) + "…" : sb.toString();
    }

    private static String describe(List<Perm> perms) {
        return String.join(", ", perms.stream().map(p -> p.role().name() + "." + p.name()).toList());
    }

    public static String grantKey(RoleCode role, Perm perm) {
        return role.name() + "|" + perm.name();
    }
}
