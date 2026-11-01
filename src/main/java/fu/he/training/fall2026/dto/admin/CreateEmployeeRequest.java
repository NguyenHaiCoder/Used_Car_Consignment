package fu.he.training.fall2026.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateEmployeeRequest {

    @NotBlank
    @Size(max = 160)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(max = 30)
    private String phone;

    /** RoleCode name, e.g. ADMIN, MANAGER, … */
    @NotBlank
    private String role;

    /** Optional display label from select (ignored if role code set). */
    private String roleLabel;
}
