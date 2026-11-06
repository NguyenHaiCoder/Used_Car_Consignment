package fu.he.training.fall2026.validation;

import fu.he.training.fall2026.dto.buyer.BuyerRequest;
import java.util.Map;
import org.springframework.validation.BindingResult;

/** Maps shared buyer field rules to errors on the web form. */
public final class BuyerFormValidation {
    private BuyerFormValidation() { }

    public static void validateForCreate(BuyerRequest input, BindingResult binding) {
        validateFields(input, binding);
        if (!binding.hasFieldErrors("consentStored") && !input.isConsentStored()) {
            binding.rejectValue("consentStored", "buyer.invalid", BuyerValidation.CONSENT_REQUIRED);
        }
    }

    public static void validateForUpdate(BuyerRequest input, BindingResult binding) {
        validateFields(input, binding);
    }

    private static void validateFields(BuyerRequest input, BindingResult binding) {
        for (Map.Entry<String, String> error : BuyerValidation.errors(
                input.getFullName(), input.getPhone(), input.getEmail(), input.getAddress()).entrySet()) {
            if (!binding.hasFieldErrors(error.getKey())) {
                binding.rejectValue(error.getKey(), "buyer.invalid", error.getValue());
            }
        }
    }
}
