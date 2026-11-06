package fu.he.training.fall2026.dto.inquiry;

import fu.he.training.fall2026.validation.InquiryIdValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InquiryIdValidator.class)
public @interface ValidInquiryId {
    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
