package fu.he.training.fall2026.dto.sales;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class ViewingDateTimeValidator implements ConstraintValidator<ValidViewingDateTime, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // @NotBlank reports the missing value.
        try {
            LocalDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
