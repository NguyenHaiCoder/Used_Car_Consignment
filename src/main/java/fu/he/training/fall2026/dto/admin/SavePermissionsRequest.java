package fu.he.training.fall2026.dto.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Each entry: roleCode|permissionCode (e.g. ADMIN|VIEW_WORK).
 * Bound from checkbox name="grants" value="ADMIN|VIEW_WORK".
 */
@Getter
@Setter
@NoArgsConstructor
public class SavePermissionsRequest {

    private List<String> grants = new ArrayList<>();
}
