package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.mail.OtpMailRequest;

public interface MailService {

    void sendOtpEmail(OtpMailRequest request);
}
