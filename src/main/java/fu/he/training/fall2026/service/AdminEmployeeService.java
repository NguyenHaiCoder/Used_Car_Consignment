package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.admin.CreateEmployeeRequest;
import fu.he.training.fall2026.dto.admin.UpdateEmployeeRequest;
import fu.he.training.fall2026.entity.auth.User;
import java.util.List;
import java.util.Set;

public interface AdminEmployeeService {

    List<User> listEmployees();

    User getEmployee(Long id);

    User createEmployee(CreateEmployeeRequest request, User actor);

    User updateEmployee(Long id, UpdateEmployeeRequest request, User actor);

    User toggleActive(Long id, User actor);

    void deleteEmployee(Long id, User actor);

    /** Tài khoản không còn hồ sơ nghiệp vụ, được phép xóa vĩnh viễn. */
    Set<Long> permanentlyDeletableIds();
}
