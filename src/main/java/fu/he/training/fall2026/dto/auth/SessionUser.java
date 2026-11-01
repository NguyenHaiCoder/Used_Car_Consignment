package fu.he.training.fall2026.dto.auth;

import fu.he.training.fall2026.entity.enums.RoleCode;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User gắn trong HttpSession sau OTP login (theo mẫu FUCarRentingSystem SessionUser).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser implements Serializable {

    private Long userId;
    private String email;
    private String fullName;
    private RoleCode roleCode;
    private String roleName;

    public boolean hasRole(RoleCode code) {
        return roleCode == code;
    }
}
