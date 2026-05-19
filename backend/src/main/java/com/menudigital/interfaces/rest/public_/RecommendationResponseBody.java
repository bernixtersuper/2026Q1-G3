package com.menudigital.interfaces.rest.public_;

import java.util.List;

public class RecommendationResponseBody {

    public List<String> recommended_items;

    public RecommendationResponseBody(List<String> recommended_items) {
        this.recommended_items = recommended_items;
    }
}
