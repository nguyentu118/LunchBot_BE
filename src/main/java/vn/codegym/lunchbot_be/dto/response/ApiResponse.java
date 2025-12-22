package vn.codegym.lunchbot_be.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ Response chung cho tất cả các API endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    // Status của response (true = success, false = error)
    private boolean success;

    // Message mô tả kết quả
    private String message;

    // Data trả về (nếu có)
    private Object data;

    // Timestamp của request
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Error code (nếu có lỗi)
    private String errorCode;

    // Constructor cho case đơn giản (chỉ success + message)
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor với data
    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // ✅ Factory methods để dễ dùng
    public static ApiResponse success(String message) {
        return new ApiResponse(true, message);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(true, message, data);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }

    public static ApiResponse error(String message, String errorCode) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}