package fu.he.training.fall2026.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nguyễn Việt Hải: gắn lên handler (hoặc cả controller) để {@code PermissionInterceptor} kiểm tra quyền trước khi chạy.
 * Gắn trên method thì method thắng. Thao tác nằm bên trong một endpoint (ví dụ Ký so với Lưu nháp) thì gọi
 * {@link PermissionGuard#require(Perm)} ngay trong code.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    Perm value();
}
