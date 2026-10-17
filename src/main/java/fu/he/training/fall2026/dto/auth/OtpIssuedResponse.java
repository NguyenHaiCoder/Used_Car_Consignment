package fu.he.training.fall2026.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Response after OTP has been issued for an email. */
@Getter
@Builder
@AllArgsConstructor
public class OtpIssuedResponse {

    private final String email;
    private final String message;
    private final int expiresInMinutes;
}
