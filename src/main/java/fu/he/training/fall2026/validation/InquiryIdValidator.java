package fu.he.training.fall2026.validation;

import fu.he.training.fall2026.dto.inquiry.ValidInquiryId;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class InquiryIdValidator implements ConstraintValidator<ValidInquiryId, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; 
        if (!value.matches("[1-9][0-9]*")) return false;
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
