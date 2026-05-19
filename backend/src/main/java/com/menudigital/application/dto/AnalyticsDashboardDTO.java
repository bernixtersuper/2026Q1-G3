package com.menudigital.application.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsDashboardDTO {
    public int totalMenuViews;
    public int totalItemViews;
    public List<ItemVisit> topItems;
    public Map<Integer, Integer> viewsByHour;

    public static class ItemVisit {
        public String itemId;
        public int views;

        public ItemVisit(String itemId, int views) {
            this.itemId = itemId;
            this.views = views;
        }
    }
}
