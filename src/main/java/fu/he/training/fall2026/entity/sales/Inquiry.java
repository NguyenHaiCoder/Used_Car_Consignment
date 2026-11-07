package fu.he.training.fall2026.entity.sales;

import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.common.BaseEntity;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.InquiryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inquiries")
public class Inquiry extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InquiryStatus status;

    @Column(length = 40)
    private String channel;

    /** Họ tên khách từ form liên hệ public. */
    @Column(length = 160)
    private String fullName;

    /** SĐT khách từ form liên hệ public (chuẩn hóa 0xxxxxxxxx). */
    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    /** PRICE | VIEWING | TEST_DRIVE */
    @Column(length = 40)
    private String need;

    /** CCCD/CMND — Sales bổ sung khi chốt đặt cọc / bàn giao. */
    @Column(length = 20)
    private String idNumber;

    @Column(length = 500)
    private String address;

    @Column(length = 2000)
    private String notes;

    /** Đã đồng ý lưu thông tin liên hệ (form public). DEFAULT 0 cho hàng cũ. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean consentStored;
}
