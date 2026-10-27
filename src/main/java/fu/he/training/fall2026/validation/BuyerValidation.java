package fu.he.training.fall2026.validation;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared schema lengths and editable-data rules for both create and update. */
public final class BuyerValidation {
    public static final int FULL_NAME_MAX = 160;
    public static final int PHONE_MAX = 30;
    public static final int EMAIL_MAX = 180;
    public static final int ADDRESS_MAX = 255;
    // Existing NVARCHAR(500) already supports the tighter business limit; do not shrink stored data.
    public static final int ADDRESS_COLUMN_LENGTH = 500;
    public static final String CONSENT_REQUIRED = "Vui lòng xác nhận đã lưu sự đồng ý của người mua.";
    private BuyerValidation() { }

    public static Map<String, String> errors(fu.he.training.fall2026.dto.buyer.CreateBuyerRequest input) {
        return errors(input.getFullName(), input.getPhone(), input.getEmail(), input.getAddress());
    }

    public static fu.he.training.fall2026.dto.buyer.CreateBuyerRequest normalize(
            fu.he.training.fall2026.dto.buyer.CreateBuyerRequest input) {
        String email = clean(input.getEmail());
        return new fu.he.training.fall2026.dto.buyer.CreateBuyerRequest(name(input.getFullName()), input.getPhone(),
                email == null ? null : email.toLowerCase(java.util.Locale.ROOT), clean(input.getAddress()), input.getConsentStored());
    }

    public static String clean(String value) {
        return value == null || value.strip().isEmpty() ? null : value.strip();
    }

    public static String name(String value) {
        return clean(value) == null ? null : Normalizer.normalize(clean(value), Normalizer.Form.NFC);
    }

    public static Map<String, String> errors(String fullName, String phone, String email, String address) {
        Map<String, String> errors = new LinkedHashMap<>();
        check(errors, "fullName", fullName, FULL_NAME_MAX, true);
        check(errors, "phone", phone, PHONE_MAX, true);
        check(errors, "email", email, EMAIL_MAX, false);
        check(errors, "address", address, ADDRESS_MAX, false);
        String name = name(fullName);
        if (!errors.containsKey("fullName") && name != null
                && !name.matches("\\p{L}[\\p{L}\\p{M}]*(?: +\\p{L}[\\p{L}\\p{M}]*)*"))
            errors.put("fullName", "Họ tên chỉ được chứa chữ cái và khoảng trắng giữa các từ.");
        if (!errors.containsKey("phone") && phone != null && !phone.matches("0[0-9]{9,10}"))
            errors.put("phone", "Số điện thoại phải gồm 10–11 chữ số và bắt đầu bằng số 0, không chứa dấu cách.");
        String mail = clean(email);
        if (!errors.containsKey("email") && mail != null
                && !mail.matches("(?U)[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+"))
            errors.put("email", "Vui lòng nhập email đúng định dạng.");
        return errors;
    }

    private static void check(Map<String, String> errors, String field, String value, int max, boolean required) {
        if (value != null && value.length() > max)
            errors.put(field, field.equals("address") ? "Địa chỉ không được vượt quá " + max + " ký tự."
                    : "Không được nhập quá " + max + " ký tự.");
        else if (required && clean(value) == null)
            errors.put(field, "Vui lòng nhập trường bắt buộc này.");
        else if (value != null && value.codePoints().anyMatch(Character::isISOControl))
            errors.put(field, "Không được chứa ký tự điều khiển.");
    }
}
