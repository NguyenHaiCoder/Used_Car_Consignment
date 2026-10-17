package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.mail.OtpMailRequest;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.MailService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@garagepro.local}")
    private String fromAddress;

    @Value("${app.mail.from-name:GaragePro Consign}")
    private String fromName;

    @Override
    public void sendOtpEmail(OtpMailRequest request) {
        try {
            String html = buildOtpHtml(
                    request.getFullName() != null ? request.getFullName() : "bạn",
                    request.getOtpCode(),
                    request.getExpiresInMinutes());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(request.getToEmail());
            helper.setSubject("Mã OTP đăng nhập GaragePro Consign");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("[MAIL] OTP sent to {}", request.getToEmail());
        } catch (Exception ex) {
            log.error("[MAIL] Failed to send OTP to {}: {}", request.getToEmail(), ex.getMessage());
            throw new BusinessException(
                    "Không gửi được email OTP. Kiểm tra MailDev đang chạy (SMTP :1025).",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** HTML OTP nhúng trong service — không dùng file template riêng. */
    private static String buildOtpHtml(String fullName, String otpCode, int expiresInMinutes) {
        String safeName = escapeHtml(fullName);
        String safeOtp = escapeHtml(otpCode);
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"/><title>Mã OTP GaragePro</title></head>
                <body style="margin:0;padding:0;background:#f6f7f9;font-family:Segoe UI,Arial,sans-serif;color:#1a1a1a;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f6f7f9;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" style="max-width:480px;background:#ffffff;border-radius:16px;overflow:hidden;">
                        <tr>
                          <td style="background:#FF7A00;padding:20px 28px;color:#fff;font-size:20px;font-weight:700;">
                            GaragePro Consign
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px;">
                            <p style="margin:0 0 12px;font-size:16px;">Xin chào <strong>%s</strong>,</p>
                            <p style="margin:0 0 20px;color:#555;line-height:1.5;">
                              Đây là mã OTP đăng nhập của bạn. Mã hết hạn sau <strong>%d</strong> phút.
                            </p>
                            <p style="margin:0 0 8px;font-size:13px;color:#777;letter-spacing:.08em;text-transform:uppercase;">
                              Mã xác minh
                            </p>
                            <p style="margin:0 0 24px;font-size:36px;font-weight:800;letter-spacing:.35em;color:#FF7A00;">%s</p>
                            <p style="margin:0;font-size:13px;color:#888;line-height:1.5;">
                              Nếu bạn không yêu cầu đăng nhập, hãy bỏ qua email này.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, expiresInMinutes, safeOtp);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
