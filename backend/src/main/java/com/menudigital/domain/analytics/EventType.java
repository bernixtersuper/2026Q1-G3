package com.menudigital.domain.analytics;

public enum EventType {
    MENU_VIEW,
    ITEM_VIEW,
    SECTION_VIEW,
    /** @deprecated use FILTER_APPLIED — kept for backward compatibility */
    FILTER_USED,
    FILTER_APPLIED,
    CART_ITEM_ADDED,
    CART_ITEM_REMOVED,
    ORDER_SUBMITTED,
    ORDER_STATUS_CHANGED;

    public EventType normalized() {
        return this == FILTER_USED ? FILTER_APPLIED : this;
    }

    public boolean updatesAggregates() {
        return switch (normalized()) {
            case MENU_VIEW, ITEM_VIEW, SECTION_VIEW, CART_ITEM_ADDED -> true;
            default -> false;
        };
    }
}
