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

export interface AnalyticsTrends {
  days: number;
  series: TrendDailyPoint[];
  filterUsage: Record<string, number>;
  sectionEngagement: SectionAnalytics[];
  totalOrders: number;
  totalRevenue: number;
}

export interface TrendDailyPoint {
  date: string;
  orders: number;
  revenue: number;
  menuViews: number;
  itemViews: number;
  uniqueMenuSessions: number | null;
  conversionRate: number | null;
  conversionStatus: 'PRELIMINARY' | 'FINAL';
}

export interface AnalyticsExportJob {
  jobId: string;
  status: string;
  downloadUrl: string | null;
  createdAt: string | null;
  completedAt: string | null;
  errorMessage: string | null;
}

export interface AnalyticsSummary {
  period: string;
  ordersToday: number;
  ordersYesterday: number;
  revenueToday: number;
  avgTicket: number;
  menuViewsToday: number;
  menuViewsYesterday: number;
  conversionRate: number | null;
  conversionStatus: 'PRELIMINARY' | 'FINAL';
  conversionNote: string | null;
  activeTables: number;
  peakHourToday: number | null;
}

export interface AnalyticsMenuData {
  topSoldItems: TopSoldItem[];
  topViewedItems: TopViewedItem[];
  viewedVsSold: ViewedVsSoldItem[];
}

export interface TopSoldItem {
  itemId: string;
  itemName: string;
  quantitySold: number;
  revenue: number;
}

export interface TopViewedItem {
  itemId: string;
  itemName: string;
  viewCount: number;
}

export interface ViewedVsSoldItem {
  itemId: string;
  itemName: string;
  viewCount: number;
  quantitySold: number;
}

export interface AnalyticsOperations {
  ordersHeatmap: Record<string, Record<number, number>>;
  viewsHeatmap: Record<string, Record<number, number>>;
  peakHourToday: number | null;
  activeTables: number;
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
  totalEventsLast5Min: number;
  totalEventsLast60Min: number;
  totalOrdersLast5Min: number;
  totalOrdersLast60Min: number;
}

export interface BucketCount {
  bucketStart: string;
  eventCount: number;
  orderCount: number;
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

// --- Owner menu insights ---

export type MenuEngineeringClass = 'STAR' | 'PLOWHORSE' | 'PUZZLE' | 'DOG';

export interface ItemPairRow {
  itemAId: string;
  itemAName: string;
  itemBId: string;
  itemBName: string;
  coOccurrenceCount: number;
  support: number;
  lift: number;
}

export interface MenuItemClass {
  itemId: string;
  itemName: string;
  quantitySold: number;
  revenue: number;
  classification: MenuEngineeringClass;
}

export interface MenuEngineering {
  avgQuantity: number;
  avgRevenue: number;
  items: MenuItemClass[];
}

export interface ModifierRow {
  modifierName: string;
  timesSelected: number;
  revenue: number;
}

export interface MenuInsights {
  days: number;
  ordersAnalyzed: number;
  avgBasketSize: number;
  distinctItemsSold: number;
  frequentlyBoughtTogether: ItemPairRow[];
  menuEngineering: MenuEngineering;
  topModifiers: ModifierRow[];
}
