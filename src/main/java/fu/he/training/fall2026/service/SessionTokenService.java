package fu.he.training.fall2026.service;

import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.auth.UserToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public interface SessionTokenService {

    UserToken createLoginSession(User user, HttpSession session, HttpServletRequest request);

    void revokeCurrentSession(HttpSession session);

    void revokeToken(Long tokenId, User actor);
}
