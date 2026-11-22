package com.pranit.praya.model;

import com.pranit.praya.constant.ApiConstants;
import lombok.Builder;

/**
 * Strict JSON payload envelope returned by all AI Agent tools.
 */
@Builder
public record ToolResponse<T>(
        String status,
        DataSourceType source,
        int count,
        T data,
        String message
) {

    public static <T> ToolResponse<T> success(DataSourceType source, int count, T data) {
        return ToolResponse.<T>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(source)
                .count(count)
                .data(data)
                .message("Data retrieved successfully.")
                .build();
    }

    public static <T> ToolResponse<T> notAvailable(DataSourceType source, String userFriendlyMessage) {
        return ToolResponse.<T>builder()
                .status(ApiConstants.STATUS_NOT_AVAILABLE)
                .source(source)
                .count(0)
                .data(null)
                .message(userFriendlyMessage != null && !userFriendlyMessage.isBlank()
                        ? userFriendlyMessage
                        : "Data is currently not available.")
                .build();
    }

    public static <T> ToolResponse<T> error(DataSourceType source, String userFriendlyMessage) {
        return ToolResponse.<T>builder()
                .status(ApiConstants.STATUS_ERROR)
                .source(source)
                .count(0)
                .data(null)
                .message(userFriendlyMessage != null && !userFriendlyMessage.isBlank()
                        ? userFriendlyMessage
                        : "Something went wrong while retrieving live data. Please try again later.")
                .build();
    }
}
