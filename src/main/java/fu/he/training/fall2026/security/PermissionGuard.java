package fu.he.training.fall2026.security;

import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.exception.PermissionDeniedException;
import fu.he.training.fall2026.repository.RolePermissionRepository;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Nguyễn Việt Hải: trả lời "role này có đang được bật chức năng X không".
 * Đọc bảng role_permissions một lần rồi giữ trong bộ nhớ, Admin lưu ma trận thì {@link #invalidate()}.
 */
@Component
@RequiredArgsConstructor
public class PermissionGuard {

    private final RolePermissionRepository rolePermissionRepository;

    private volatile Map<RoleCode, Set<Perm>> grants;

    public boolean has(RoleCode role, Perm perm) {
        if (role == null || perm == null || perm.role() != role) {
            return false;
        }
        return perm.locked() || grants().getOrDefault(role, Set.of()).contains(perm);
    }

    /** Kiểm tra theo role của người đang đăng nhập trong request hiện tại. */
    public boolean currentHas(Perm perm) {
        return has(currentRole(), perm);
    }

    /** Dùng trong controller khi một endpoint làm nhiều việc, ví dụ Lưu nháp so với Ký hợp đồng. */
    public void require(Perm perm) {
        if (!currentHas(perm)) {
            throw new PermissionDeniedException();
        }
    }

    public void invalidate() {
        grants = null;
    }

    private Map<RoleCode, Set<Perm>> grants() {
        Map<RoleCode, Set<Perm>> snapshot = grants;
        if (snapshot == null) {
            snapshot = load();
            grants = snapshot;
        }
        return snapshot;
    }

    /** findAllFetched join fetch sẵn role và permission nên không cần transaction. */
    private Map<RoleCode, Set<Perm>> load() {
        Map<RoleCode, Set<Perm>> map = new EnumMap<>(RoleCode.class);
        rolePermissionRepository.findAllFetched().forEach(rp -> {
            Perm perm = Perms.byCode(rp.getPermission().getCode());
            if (perm != null) {
                map.computeIfAbsent(rp.getRole().getCode(), k -> EnumSet.noneOf(Perm.class)).add(perm);
            }
        });
        map.replaceAll((k, v) -> Collections.unmodifiableSet(v));
        return Collections.unmodifiableMap(map);
    }

    private static RoleCode currentRole() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return SessionAuthUtils.getRoleCode(request.getSession(false));
    }
}
