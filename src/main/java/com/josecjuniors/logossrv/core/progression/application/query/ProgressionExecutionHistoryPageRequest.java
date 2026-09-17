package com.josecjuniors.logossrv.core.progression.application.query;

public record ProgressionExecutionHistoryPageRequest(int page, int size) {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public ProgressionExecutionHistoryPageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
