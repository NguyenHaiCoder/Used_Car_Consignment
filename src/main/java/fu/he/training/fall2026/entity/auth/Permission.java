package fu.he.training.fall2026.entity.auth;

import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.enums.RoleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Nguyễn Việt Hải: một chức năng trong danh mục phân quyền, đồng bộ từ enum {@code security.Perm}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Nationalized
    @Column(nullable = false, length = 160)
    private String name;

    @Nationalized
    @Column(length = 500)
    private String description;

    /** Nhóm chức năng hiển thị trên màn phân quyền, ví dụ "Hợp đồng bán xe". */
    @Nationalized
    @Column(name = "module_name", length = 120)
    private String moduleName;

    /** Role sở hữu chức năng. Chỉ role này được bật, không cấp chéo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_role", length = 30)
    private RoleCode ownerRole;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
