export interface Restaurant {
  id: string;
  name: string;
  slug: string;
}

export interface MenuSection {
  id: string;
  name: string;
  displayOrder: number;
  items: MenuItem[];
}

export interface MenuItem {
  id: string;
  sectionId: string;
  name: string;
  description: string;
  price: string;
  imageUrl: string;
  available: boolean;
  dietaryTags: DietaryTag[];
  displayOrder: number;
}

export type DietaryTag = 'VEGAN' | 'VEGETARIAN' | 'GLUTEN_FREE' | 'DAIRY_FREE';

export interface MenuResponse {
  tenantId: string;
  restaurantName: string;
  slug: string;
  sections: MenuSection[];
}

export interface AnalyticsDashboard {
  totalMenuViewsLast30Days: number;
  totalMenuViewsToday: number;
  uniqueSessionsLast30Days: number;
  avgSessionDepth: number;
  dailyViews: DailyViewCount[];
  hourlyHeatmap: Record<string, Record<number, number>>;
  topItems: ItemAnalytics[];
  sectionEngagement: SectionAnalytics[];
  filterUsage: Record<string, number>;
  peakHourOfDay: number;
  peakDayOfWeek: string;
}

export interface DailyViewCount {
  date: string;
  menuViews: number;
  itemViews: number;
}

export interface ItemAnalytics {
  itemId: string;
  itemName: string;
  viewCount: number;
  viewRate: number;
  trending: boolean;
}

export interface SectionAnalytics {
  sectionId: string;
  sectionName: string;
  viewCount: number;
}

export interface RealtimeAnalytics {
  buckets: BucketCount[];
  totalLast5Min: number;
  totalLast60Min: number;
}

export interface BucketCount {
  bucketStart: string;
  count: number;
}

export interface SessionResponse {
  tenantId: string;
  restaurantName: string;
}

export interface CreateSectionRequest {
  name: string;
  displayOrder: number;
}

export interface CreateItemRequest {
  sectionId: string;
  name: string;
  description: string;
  price: string;
  imageUrl: string;
  dietaryTags: DietaryTag[];
  displayOrder: number;
}

export type ModifierType = 'EXTRA' | 'REMOVAL' | 'SUBSTITUTION' | 'SIZE';

export interface Modifier {
  id: string;
  menuItemId: string;
  name: string;
  priceAdjustment: string;
  modifierType: ModifierType;
  available: boolean;
  displayOrder: number;
}

export interface CreateModifierRequest {
  menuItemId: string;
  name: string;
  priceAdjustment: string;
  modifierType: ModifierType;
  displayOrder: number;
}

export interface UpdateModifierRequest {
  name: string;
  priceAdjustment: string;
  modifierType: ModifierType;
  available: boolean;
  displayOrder: number;
}
