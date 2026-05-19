CREATE TABLE menu_sections (
    id            UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sections_restaurant ON menu_sections(restaurant_id);

CREATE TABLE menu_items (
    id            UUID PRIMARY KEY,
    section_id    UUID NOT NULL REFERENCES menu_sections(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    price         NUMERIC(10,2) NOT NULL,
    image_url     TEXT,
    available     BOOLEAN NOT NULL DEFAULT TRUE,
    dietary_tags  TEXT[] NOT NULL DEFAULT '{}',
    display_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_items_restaurant ON menu_items(restaurant_id);
CREATE INDEX idx_items_section ON menu_items(section_id);
