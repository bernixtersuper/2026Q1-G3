package com.menudigital.interfaces.rest.public_;

import java.util.List;

/**
 * Body JSON para POST /api/menu/{slug}/recommendations (snake_case en wire).
 */
public class RecommendationRequestBody {

    public List<String> items_in_cart;
    public List<String> menu_item_ids;
}
