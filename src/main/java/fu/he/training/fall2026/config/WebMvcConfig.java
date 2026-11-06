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

    private final RoleAuthInterceptor roleAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleAuthInterceptor)
                .addPathPatterns(
                        "/admin/**",
                        "/manager/**",
                        "/consignments/**",
                        "/consignment/**",
                        "/inspector/**",
                        "/sales/**",
                        "/finance/**",
                        "/handover/**",
                        "/documents/**")
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error/**");
    }
}
