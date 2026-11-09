package fu.he.training.fall2026.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

/**
 * Nguyễn Việt Hải: bị chặn quyền thì quay lại trang người dùng vừa đứng và hiện toast.
 * Không có trang hợp lệ để quay lại (gõ thẳng URL, vừa đăng nhập xong) thì hiện trang 403.
 */
public final class PermissionDenial {

    /** Tên flash attribute, fragment common/access-toast đọc tên này. */
    public static final String FLASH_KEY = "permissionDenied";

    private PermissionDenial() {
    }

    /** Đường dẫn (không gồm context path) để redirect về, hoặc null nếu phải hiện trang 403. */
    public static String backTarget(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(referer.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (uri.getHost() == null || !uri.getHost().equalsIgnoreCase(request.getServerName())
                || portOf(uri) != request.getServerPort()) {
            return null;
        }
        String context = request.getContextPath();
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        if (!path.startsWith(context)) {
            return null;
        }
        path = path.substring(context.length());
        if (path.isEmpty()) {
            path = "/";
        }
        if (path.startsWith("/auth")) {
            return null;
        }
        // GET bị chặn mà referer chính là trang đó thì redirect về sẽ lặp, nên hiện 403.
        String current = request.getRequestURI().substring(context.length());
        if ("GET".equalsIgnoreCase(request.getMethod()) && path.equals(current)) {
            return null;
        }
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }

    private static int portOf(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
