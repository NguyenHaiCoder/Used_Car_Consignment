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
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inquiries")
public class Inquiry extends BaseEntity {

    @Nationalized
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

    @Nationalized
    @Column(length = 40)
    private String channel;

    /** Họ tên khách từ form liên hệ public. */
    @Nationalized
    @Column(length = 160)
    private String fullName;

    /** SĐT khách từ form liên hệ public (chuẩn hóa 0xxxxxxxxx). */
    @Nationalized
    @Column(length = 30)
    private String phone;

    @Nationalized
    @Column(length = 180)
    private String email;

    /** PRICE | VIEWING | TEST_DRIVE */
    @Nationalized
    @Column(length = 40)
    private String need;

    /** CCCD/CMND — Sales bổ sung khi chốt đặt cọc / bàn giao. */
    @Nationalized
    @Column(length = 20)
    private String idNumber;

    @Nationalized
    @Column(length = 500)
    private String address;

    @Nationalized
    @Column(length = 2000)
    private String notes;

    /** Lý do đóng tư vấn (khi status = CLOSED). */
    @Nationalized
    @Column(length = 120)
    private String closeReason;

    /** Đã đồng ý lưu thông tin liên hệ (form public). DEFAULT 0 cho hàng cũ. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean consentStored;
}
