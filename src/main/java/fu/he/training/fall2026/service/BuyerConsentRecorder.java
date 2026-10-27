package fu.he.training.fall2026.service;

import fu.he.training.fall2026.entity.auth.AuditEvent;
import fu.he.training.fall2026.repository.AuditEventRepository;
import fu.he.training.fall2026.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BuyerConsentRecorder {
    private final AuditEventRepository auditRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(long buyerId) {
        var event = new AuditEvent();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated())
            event.setActor(userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null));
        event.setAction("BUYER_CONSENT_STORAGE_CONFIRMED");
        event.setEntityType("Buyer");
        event.setEntityId(buyerId);
        event.setOccurredAt(Instant.now());
        event.setDetail("Employee confirmed that buyer consent was stored; this attestation is not independent evidence of customer consent.");
        auditRepository.saveAndFlush(event);
    }
}
