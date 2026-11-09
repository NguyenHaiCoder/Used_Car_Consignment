package fu.he.training.fall2026.exception;

/** Nguyễn Việt Hải: role của người dùng đang bị tắt chức năng này trong ma trận phân quyền. */
public class PermissionDeniedException extends RuntimeException {

    public static final String MESSAGE = "Bạn không có quyền thực hiện chức năng này, vui lòng liên hệ quản trị viên.";

    public PermissionDeniedException() {
        super(MESSAGE);
    }
}
