package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.auth.AuthSessionResponse;
import fu.he.training.fall2026.dto.auth.LoginOtpRequest;
import fu.he.training.fall2026.dto.auth.OtpIssuedResponse;
import fu.he.training.fall2026.dto.auth.VerifyOtpRequest;
import fu.he.training.fall2026.dto.mail.OtpMailRequest;
import fu.he.training.fall2026.entity.auth.OtpVerification;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.enums.RoleCode;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.repository.OtpVerificationRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.service.AuthService;
import fu.he.training.fall2026.service.MailService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.otp.accept-master:false}")
    private boolean acceptMasterOtp;

    @Value("${app.auth.otp.master-code:123456}")
    private String masterOtpCode;

    @Override
    @Transactional
    public OtpIssuedResponse requestOtp(LoginOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException(
                        "Email chưa được đăng ký trong hệ thống.", HttpStatus.NOT_FOUND));

        if (!user.isActive()) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa.", HttpStatus.FORBIDDEN);
        }

        String code = generateOtp();
        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setEmail(email);
        otp.setOtpCode(code);
        otp.setExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
        otp.setAttemptCount(0);
        otpVerificationRepository.save(otp);

        mailService.sendOtpEmail(OtpMailRequest.builder()
                .toEmail(email)
                .fullName(user.getFullName())
                .otpCode(code)
                .expiresInMinutes(OTP_TTL_MINUTES)
                .build());

        return OtpIssuedResponse.builder()
                .email(email)
                .message("Mã OTP đã được gửi tới email của bạn.")
                .expiresInMinutes(OTP_TTL_MINUTES)
                .build();
    }

    @Override
    @Transactional
    public AuthSessionResponse verifyOtpAndLogin(VerifyOtpRequest request, HttpSession session) {
        String email = normalizeEmail(request.getEmail());
        String otpCode = request.getOtpCode() == null ? "" : request.getOtpCode().trim();

        OtpVerification otp = otpVerificationRepository
                .findFirstByEmailIgnoreCaseAndConsumedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(
                        "Chưa có mã OTP hợp lệ. Vui lòng đăng nhập lại để nhận mã mới."));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Mã OTP đã hết hạn. Vui lòng gửi lại mã.");
        }
        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessException("Đã vượt quá số lần thử OTP. Vui lòng gửi lại mã.");
        }

        boolean masterOk = acceptMasterOtp && masterOtpCode != null && masterOtpCode.equals(otpCode);
        boolean matched = otpCode.equals(otp.getOtpCode()) || masterOk;
        if (!matched) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpVerificationRepository.save(otp);
            throw new BusinessException("Mã OTP không chính xác. Vui lòng thử lại.");
        }

        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException(
                        "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa.", HttpStatus.FORBIDDEN);
        }

        otp.setConsumedAt(Instant.now());
        otpVerificationRepository.save(otp);

        user.setEmailVerified(true);
        userRepository.save(user);

        SessionAuthUtils.bindLogin(session, user);
        return toSessionResponse(user);
    }

    @Override
    public void logout(HttpSession session) {
        if (session != null) {
            SessionAuthUtils.clearLogin(session);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthSessionResponse currentSession(HttpSession session) {
        if (!SessionAuthUtils.isLoggedIn(session)) {
            return null;
        }
        Long userId = SessionAuthUtils.getUserId(session);
        return userRepository.findByIdWithRole(userId)
                .map(this::toSessionResponse)
                .orElse(null);
    }

    @Override
    public RoleCode currentRole(HttpSession session) {
        return SessionAuthUtils.getRoleCode(session);
    }

    private AuthSessionResponse toSessionResponse(User user) {
        RoleCode roleCode = user.getRole().getCode();
        return AuthSessionResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleCode(roleCode)
                .roleName(user.getRole().getName())
                .homePath(SessionAuthUtils.homePathForRole(roleCode))
                .build();
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int value = secureRandom.nextInt(bound / 10, bound);
        return String.valueOf(value);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
