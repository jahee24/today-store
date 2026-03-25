package today_store.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class PageRequest {

    @Min(value = 1, message = "Page number should be greater than 1")
    private int page = 1;

    @Min(value = 1, message = "Page size should be greater than 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private int size = 10;

    public Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(page - 1, size);
    }
}
