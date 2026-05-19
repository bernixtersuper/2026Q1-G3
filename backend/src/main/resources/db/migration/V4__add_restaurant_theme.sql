-- Add theme customization to restaurants
ALTER TABLE restaurants ADD COLUMN theme JSONB NOT NULL DEFAULT '{
  "primaryColor": "#8b5cf6",
  "secondaryColor": "#14b8a6",
  "accentColor": "#f59e0b",
  "backgroundColor": "#ffffff",
  "textColor": "#1f2937",
  "cardBackground": "#ffffff",
  "gradientStart": "#8b5cf6",
  "gradientEnd": "#6366f1",
  "gradientDirection": "to-br",
  "fontFamily": "Inter",
  "borderRadius": "lg",
  "logoUrl": null,
  "bannerUrl": null,
  "showGradientHeader": true
}'::jsonb;

-- Add table management
CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    table_number VARCHAR(50) NOT NULL,
    table_name VARCHAR(100),
    capacity INT DEFAULT 4,
    qr_code_token VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(restaurant_id, table_number)
);

CREATE INDEX idx_tables_restaurant ON restaurant_tables(restaurant_id);
CREATE INDEX idx_tables_qr_token ON restaurant_tables(qr_code_token);

-- Table sessions for secure ordering
CREATE TABLE table_sessions (
    id UUID PRIMARY KEY,
    table_id UUID NOT NULL REFERENCES restaurant_tables(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    session_code VARCHAR(6) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(50) DEFAULT 'qr_scan'
);

CREATE INDEX idx_sessions_table ON table_sessions(table_id);
CREATE INDEX idx_sessions_code ON table_sessions(session_code);
CREATE INDEX idx_sessions_active ON table_sessions(is_active, expires_at);
