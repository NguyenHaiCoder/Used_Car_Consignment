package fu.he.training.fall2026.config;

import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Session + role-based access (Hải = ADMIN → /admin/**).
 */
@Configuration
@EnableWebSecurity

public class SecurityConfig {

    static final RequestMatcher SHARED_INQUIRY_READ = request -> "GET".equals(request.getMethod())
            && ("/sales/inquiries".equals(request.getServletPath())
                || request.getServletPath().matches("/sales/inquiries/[0-9]+"));

    private final SessionAuthenticationFilter sessionAuthenticationFilter = new SessionAuthenticationFilter();

    @Bean
    @Order(0)
    public SecurityFilterChain buyerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/sales/buyers", "/sales/buyers/**",
                        "/sales/inquiries", "/sales/inquiries/**", "/sales/viewings", "/sales/viewings/**",
                        "/sales/reservations", "/sales/reservations/**", "/sales/contracts", "/sales/contracts/**",
                        "/reservations", "/reservations/**", "/sales-contracts", "/sales-contracts/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SHARED_INQUIRY_READ)
                        .hasAnyRole("SALES", "MANAGER")
                        .anyRequest().hasRole("SALES"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, error) ->
                                response.sendRedirect(request.getContextPath() + "/?loginRequired=1"))
                        .accessDeniedHandler((request, response, error) ->
                                response.sendRedirect(request.getContextPath() + "/?accessDenied=1")))
                .addFilterAfter(sessionAuthenticationFilter, SecurityContextHolderFilter.class)
                .build();
    }

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
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect("/?accessDenied=1")))
                .csrf(csrf -> csrf.disable())
                .addFilterAfter(sessionAuthenticationFilter, SecurityContextHolderFilter.class);

        return http.build();
    }

    private static final class SessionAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            HttpSession session = request.getSession(false);
            if (session != null
                    && SessionAuthUtils.isLoggedIn(session)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                SessionAuthUtils.restoreSecurityContext(session);
            }
            filterChain.doFilter(request, response);
        }
    }
    
}
