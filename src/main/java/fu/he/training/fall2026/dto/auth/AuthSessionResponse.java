package fu.he.training.fall2026.dto.auth;

import fu.he.training.fall2026.entity.enums.RoleCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Response after successful login (session already bound). */
@Getter
@Builder
@AllArgsConstructor
public class AuthSessionResponse {

    private final Long userId;
    private final String email;
    private final String fullName;
    private final RoleCode roleCode;
    private final String roleName;
    private final String homePath;
}
