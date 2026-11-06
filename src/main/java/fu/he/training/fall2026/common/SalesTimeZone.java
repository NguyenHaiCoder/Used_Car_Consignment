package fu.he.training.fall2026.common;

import java.time.ZoneId;

/** Shared business time zone for sales dates displayed outside the service layer. */
public final class SalesTimeZone {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private SalesTimeZone() { }
}
