package fu.he.training.fall2026.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC + RoleAuthInterceptor (mẫu FUCarRentingSystem WebMvcConfig).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String[] ROLE_AREAS = {
            "/admin/**",
            "/manager/**",
            "/consignments/**",
            "/consignment/**",
            "/inspector/**",
            "/sales/**",
            "/finance/**",
            "/handover/**",
            "/documents/**"};

    private final RoleAuthInterceptor roleAuthInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleAuthInterceptor)
                .addPathPatterns(ROLE_AREAS)
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error/**");
        // Nguyễn Việt Hải: đăng ký sau nên chạy sau, lúc này đã chắc người dùng đăng nhập đúng role.
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns(ROLE_AREAS);
    }
}
