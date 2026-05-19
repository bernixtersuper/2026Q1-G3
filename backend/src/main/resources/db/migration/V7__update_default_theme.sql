-- Update default theme to be more professional/neutral
ALTER TABLE restaurants ALTER COLUMN theme SET DEFAULT '{
  "primaryColor": "#374151",
  "secondaryColor": "#6b7280",
  "accentColor": "#9ca3af",
  "backgroundColor": "#f9fafb",
  "textColor": "#111827",
  "cardBackground": "#ffffff",
  "gradientStart": "#374151",
  "gradientEnd": "#1f2937",
  "gradientDirection": "to-br",
  "fontFamily": "Inter",
  "borderRadius": "lg",
  "logoUrl": null,
  "bannerUrl": null,
  "showGradientHeader": false
}'::jsonb;
