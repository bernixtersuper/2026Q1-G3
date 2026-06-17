package com.menudigital.domain.analytics;

public enum SalesPeriod {
    TODAY("today"),
    DAYS_30("30d"),
    ALL_TIME("all");

    private final String paramValue;

    SalesPeriod(String paramValue) {
        this.paramValue = paramValue;
    }

    public String paramValue() {
        return paramValue;
    }

    public static SalesPeriod fromParam(String param) {
        if (param == null || param.isBlank()) {
            return TODAY;
        }
        String normalized = param.trim().toLowerCase();
        for (SalesPeriod period : values()) {
            if (period.paramValue.equals(normalized)) {
                return period;
            }
        }
        return switch (normalized) {
            case "30", "last_30_days", "last-30-days" -> DAYS_30;
            case "all_time", "all-time", "lifetime" -> ALL_TIME;
            default -> TODAY;
        };
    }
}
