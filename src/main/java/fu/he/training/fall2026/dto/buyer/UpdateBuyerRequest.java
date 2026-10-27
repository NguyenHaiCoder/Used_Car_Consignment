package fu.he.training.fall2026.dto.buyer;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class UpdateBuyerRequest {
    private Map<String, Object> fields = Collections.emptyMap();

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public UpdateBuyerRequest(Map<String, Object> fields) {
        setFields(fields);
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
}