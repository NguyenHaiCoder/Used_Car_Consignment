package fu.he.training.fall2026.config;

import fu.he.training.fall2026.exception.PermissionDeniedException;
import fu.he.training.fall2026.security.PermissionDenial;
import fu.he.training.fall2026.security.PermissionGuard;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Nguyễn Việt Hải: chạy sau {@link RoleAuthInterceptor} (đã chắc đúng role), kiểm tra chức năng
 * {@link RequirePermission} của handler có đang bật cho role đó không.
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionGuard permissionGuard;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (required == null || permissionGuard.currentHas(required.value())) {
            return true;
        }

        String back = PermissionDenial.backTarget(request);
        if (back == null) {
            // GlobalExceptionHandler hiện trang 403 kèm thông báo.
            throw new PermissionDeniedException();
        }
        String location = request.getContextPath() + back;
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put(PermissionDenial.FLASH_KEY, PermissionDeniedException.MESSAGE);
        RequestContextUtils.saveOutputFlashMap(location, request, response);
        response.sendRedirect(location);
        return false;
    }
}
