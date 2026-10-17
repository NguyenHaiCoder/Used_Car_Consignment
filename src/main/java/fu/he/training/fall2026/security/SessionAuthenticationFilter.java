package fu.he.training.fall2026.security;

import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restores Spring Security authentication from {@link HttpSession} attributes
 * set by {@link SessionAuthUtils#bindLogin}.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

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
