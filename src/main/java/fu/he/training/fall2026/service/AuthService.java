package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.auth.AuthSessionResponse;
import fu.he.training.fall2026.dto.auth.LoginOtpRequest;
import fu.he.training.fall2026.dto.auth.OtpIssuedResponse;
import fu.he.training.fall2026.dto.auth.VerifyOtpRequest;
import fu.he.training.fall2026.entity.enums.RoleCode;
import jakarta.servlet.http.HttpSession;

public interface AuthService {

    OtpIssuedResponse requestOtp(LoginOtpRequest request);

    AuthSessionResponse verifyOtpAndLogin(VerifyOtpRequest request, HttpSession session);

    void logout(HttpSession session);

    AuthSessionResponse currentSession(HttpSession session);

    RoleCode currentRole(HttpSession session);
}
