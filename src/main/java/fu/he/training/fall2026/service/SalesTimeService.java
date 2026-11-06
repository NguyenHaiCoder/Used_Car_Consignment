package fu.he.training.fall2026.service;

import fu.he.training.fall2026.common.SalesTimeZone;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Shared business time for reservations, contracts and viewing schedules. */
@Service
public class SalesTimeService {
    public static final ZoneId ZONE = SalesTimeZone.ZONE;

    private final Clock clock;

    public SalesTimeService() {
        this(Clock.systemUTC());
    }

    public SalesTimeService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(ZONE));
    }

    public Instant now() {
        return clock.instant();
    }
}
