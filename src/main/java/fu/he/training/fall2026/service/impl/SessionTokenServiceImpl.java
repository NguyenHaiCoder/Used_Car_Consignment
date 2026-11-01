package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.auth.UserToken;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.UserTokenRepository;
import fu.he.training.fall2026.service.AuditService;
import fu.he.training.fall2026.service.SessionTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionTokenServiceImpl implements SessionTokenService {

    public static final String SESSION_TOKEN_ATTR = "AUTH_SESSION_TOKEN";
    private static final String TOKEN_TYPE = "SESSION";
    private static final int TTL_HOURS = 12;

    private final UserTokenRepository userTokenRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public UserToken createLoginSession(User user, HttpSession session, HttpServletRequest request) {
        UserToken token = new UserToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setTokenType(TOKEN_TYPE);
        token.setExpiresAt(Instant.now().plus(TTL_HOURS, ChronoUnit.HOURS));
        if (request != null) {
            String ua = request.getHeader("User-Agent");
            token.setUserAgent(ua != null && ua.length() > 255 ? ua.substring(0, 255) : ua);
            token.setIpAddress(request.getRemoteAddr());
        }
        userTokenRepository.save(token);
        session.setAttribute(SESSION_TOKEN_ATTR, token.getToken());
        return token;
    }

    @Override
    @Transactional
    public void revokeCurrentSession(HttpSession session) {
        if (session == null) {
            return;
        }
        Object raw = session.getAttribute(SESSION_TOKEN_ATTR);
        if (raw == null) {
            return;
        }
        userTokenRepository.findByTokenAndRevokedAtIsNull(raw.toString())
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    userTokenRepository.save(token);
                });
    }

    @Override
    @Transactional
    public void revokeToken(Long tokenId, User actor) {
        UserToken token = userTokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiên.", HttpStatus.NOT_FOUND));
        if (token.getRevokedAt() != null) {
            throw new BusinessException("Phiên đã bị thu hồi.");
        }
        token.setRevokedAt(Instant.now());
        userTokenRepository.save(token);
        auditService.record(actor, "REVOKE_SESSION", "UserToken", tokenId,
                "Thu hồi phiên của " + token.getUser().getEmail());
    }
}
