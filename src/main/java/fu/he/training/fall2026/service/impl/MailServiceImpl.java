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

    /**
     * HTML OTP nhúng trong service — không dùng file template riêng.
     * Màu inline đồng bộ với css/app/palette.css để hiển thị được trong các trình đọc email.
     * Nút Sao chép mã chép thẳng vào clipboard, không mở trang nào. Cần trình xem mail cho chạy JavaScript (MailDev cho chạy);
     * Gmail, Outlook chặn JavaScript nên ở đó nút không làm gì, người dùng bấm vào mã để chọn cả 6 số rồi tự chép.
     */
    private static String buildOtpHtml(String fullName, String otpCode, int expiresInMinutes) {
        String safeName = escapeHtml(fullName);
        String safeOtp = escapeHtml(otpCode);
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8"/><title>Mã OTP GaragePro</title>
                  <style>
                    .otp-copy { cursor: pointer; transition: background-color .15s, color .15s; }
                    .otp-copy:hover, .otp-copy:focus-visible { background: #1668e3 !important; color: #fff !important; }
                    .otp-copy:focus-visible { outline: 2px solid #2f86f5; outline-offset: 2px; }
                  </style>
                  <script>
                    function copyOtp(button) {
                      var code = document.getElementById('otp-code').textContent.trim();
                      function done() {
                        button.textContent = 'Đã sao chép';
                        setTimeout(function () { button.textContent = 'Sao chép mã'; }, 2000);
                      }
                      function fallback() {
                        var range = document.createRange();
                        range.selectNodeContents(document.getElementById('otp-code'));
                        var selection = window.getSelection();
                        selection.removeAllRanges();
                        selection.addRange(range);
                        if (document.execCommand('copy')) { done(); }
                        selection.removeAllRanges();
                      }
                      if (navigator.clipboard && navigator.clipboard.writeText) {
                        navigator.clipboard.writeText(code).then(done, fallback);
                      } else {
                        fallback();
                      }
                    }
                  </script>
                </head>
                <body style="margin:0;padding:0;background:#eef5fd;font-family:Segoe UI,Arial,sans-serif;color:#0a2257;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#eef5fd;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" style="max-width:480px;background:#ffffff;border:1px solid #c9dcf3;border-radius:16px;overflow:hidden;">
                        <tr>
                          <td style="background:#05245a;padding:20px 28px;color:#fff;font-size:20px;font-weight:700;">
                            GaragePro Consign
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px;">
                            <p style="margin:0 0 12px;font-size:16px;">Xin chào <strong>%s</strong>,</p>
                            <p style="margin:0 0 20px;color:#3b5079;line-height:1.5;">
                              Đây là mã OTP đăng nhập của bạn. Mã hết hạn sau <strong>%d</strong> phút.
                            </p>
                            <p style="margin:0 0 8px;font-size:13px;color:#3b5079;letter-spacing:.08em;text-transform:uppercase;">
                              Mã xác minh
                            </p>
                            <p style="margin:0 0 12px;font-size:36px;font-weight:800;letter-spacing:.35em;color:#1668e3;">
                              <span id="otp-code" style="-webkit-user-select:all;user-select:all;">%s</span>
                            </p>
                            <button type="button" class="otp-copy" onclick="copyOtp(this)"
                                    style="display:block;margin:0 0 24px;padding:9px 18px;border:1px solid #1668e3;border-radius:8px;background:#eef5fd;color:#0b479f;font-family:inherit;font-size:14px;font-weight:600;line-height:1.4;">Sao chép mã</button>
                            <p style="margin:0;font-size:13px;color:#3b5079;line-height:1.5;">
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
