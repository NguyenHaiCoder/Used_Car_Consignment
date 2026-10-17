package fu.he.training.fall2026.dto.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Payload to render / send an OTP email via MailDev (or any SMTP). */
@Getter
@Builder
@AllArgsConstructor
public class OtpMailRequest {

    private final String toEmail;
    private final String fullName;
    private final String otpCode;
    private final int expiresInMinutes;
}
