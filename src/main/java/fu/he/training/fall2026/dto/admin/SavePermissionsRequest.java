package fu.he.training.fall2026.dto.admin;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nguyễn Việt Hải: các chức năng đang bật, mỗi phần tử dạng ROLE|MÃ (ví dụ SALES|SALES_CONTRACT_SIGN).
 * Bind từ checkbox name="grants" của màn phân quyền.
 */
@Getter
@Setter
@NoArgsConstructor
public class SavePermissionsRequest {

    private List<String> grants = new ArrayList<>();
}
