package fu.he.training.fall2026.config;

import fu.he.training.fall2026.security.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Spring Security: mỗi prefix chỉ đúng 1 role (Admin ≠ Manager).
 * {@link RoleAuthInterceptor} chặn thêm theo session {@code loginUser}.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/home",
                                "/login",
                                "/auth/sign-in",
                                "/verify-otp",
                                "/auth/login",
                                "/auth/verify-otp",
                                "/auth/resend-otp",
                                "/auth/logout",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/error",
                                "/error/**",
                                "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        .requestMatchers("/consignment/**").hasRole("CONSIGNMENT")
                        .requestMatchers("/inspector/**").hasRole("INSPECTOR")
                        .requestMatchers("/sales/**").hasRole("SALES")
                        .requestMatchers("/finance/**").hasRole("FINANCE")
                        .requestMatchers("/handover/**").hasRole("HANDOVER")
                        .requestMatchers("/documents/**").hasRole("DOCUMENTS")
                        .anyRequest().authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/?loginRequired=1"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // Sai role: cố gắng đẩy về dashboard đúng role nếu đã login
                            var auth = org.springframework.security.core.context.SecurityContextHolder
                                    .getContext().getAuthentication();
                            if (auth != null && auth.getAuthorities() != null) {
                                for (var a : auth.getAuthorities()) {
                                    String role = a.getAuthority();
                                    if (role != null && role.startsWith("ROLE_")) {
                                        try {
                                            var code = fu.he.training.fall2026.entity.enums.RoleCode
                                                    .valueOf(role.substring(5));
                                            response.sendRedirect(
                                                    fu.he.training.fall2026.util.SessionAuthUtils.homePathForRole(code)
                                                            + "?accessDenied=1");
                                            return;
                                        } catch (Exception ignored) {
                                            // fall through
                                        }
                                    }
                                }
                            }
                            response.sendRedirect("/?accessDenied=1");
                        }))
                .csrf(csrf -> csrf.disable())
                .addFilterAfter(sessionAuthenticationFilter, SecurityContextHolderFilter.class);

        return http.build();
    }
}
