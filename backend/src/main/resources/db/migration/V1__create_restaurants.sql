CREATE TABLE restaurants (
    id           UUID PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    slug         VARCHAR(100) NOT NULL UNIQUE,
    owner_email  VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restaurants_slug ON restaurants(slug);
