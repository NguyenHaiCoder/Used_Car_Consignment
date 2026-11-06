package fu.he.training.fall2026.service.impl;
import org.springframework.security.core.Authentication;

import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesAssignmentPolicy {
    private final UserRepository users;

    public User currentSalesperson() {
        return authenticatedSalesperson();
    }

    public User assignedSalesperson(Long requestedId) {
        User actor = authenticatedSalesperson();
        if (requestedId != null && !requestedId.equals(actor.getId())) {
            throw new BusinessException("Assigning another salesperson requires authorization", HttpStatus.FORBIDDEN);
        }
        return users.lockById(actor.getId())
                .filter(this::activeSalesperson)
                .orElseThrow(() -> new BusinessException("Active SALES user required", HttpStatus.FORBIDDEN));
    }

    private User authenticatedSalesperson() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        if (authentication.getAuthorities().stream().noneMatch(a -> "ROLE_SALES".equals(a.getAuthority()))) {
            throw new BusinessException("SALES role required", HttpStatus.FORBIDDEN);
        }
        return users.findByEmailIgnoreCase(authentication.getName())
                .filter(this::activeSalesperson)
                .orElseThrow(() -> new BusinessException("Active SALES user required", HttpStatus.FORBIDDEN));
    }

    private boolean activeSalesperson(User user) {
        return user.isActive() && user.getRole().getCode() == RoleCode.SALES;
    }
}
