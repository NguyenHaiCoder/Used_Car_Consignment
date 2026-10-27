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
 * Session + role-based access (Hải = ADMIN → /admin/**).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    @Bean
    @Order(0)
    public SecurityFilterChain buyerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/sales/buyers", "/sales/buyers/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SALES"))
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
}
