package fu.he.training.fall2026.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Explicit development preview only; the normal application chain is unchanged. */
@Configuration
@Profile("ui-preview")
public class    UiPreviewSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain uiPreviewSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/ui", "/ui/**", "/css/ui/**", "/js/ui/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, error) -> {
                    // Keep preview POST rejection local; do not dispatch to the protected /error route.
                    response.setStatus(403);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("UI preview supports GET pages only; no business submission is accepted.");
                }))
                .build();
    }
}
