package today_store.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data);
    }
}
