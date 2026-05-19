export interface MenuSection {
  id: string;
  name: string;
  displayOrder: number;
  items: MenuItem[];
}

export interface Modifier {
  id: string;
  name: string;
  priceAdjustment: string;
  modifierType: 'EXTRA' | 'REMOVAL' | 'SUBSTITUTION' | 'SIZE';
}

export interface MenuItem {
  id: string;
  sectionId?: string;
  name: string;
  description: string;
  price: string;
  imageUrl: string;
  available?: boolean;
  dietaryTags: string[];
  displayOrder?: number;
  modifiers?: Modifier[];
}

export type DietaryTag = 'VEGAN' | 'VEGETARIAN' | 'GLUTEN_FREE' | 'DAIRY_FREE';

export interface Theme {
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  backgroundColor: string;
  textColor: string;
  cardBackground: string;
  gradientStart: string;
  gradientEnd: string;
  gradientDirection: string;
  fontFamily: string;
  borderRadius: string;
  logoUrl: string | null;
  bannerUrl: string | null;
  showGradientHeader: boolean;
}

export interface TableMenuResponse {
  tableId: string;
  tableNumber: string;
  tableName: string;
  restaurantName: string;
  slug: string;
  theme: Theme;
  sessionCode: string | null;
  sessionId: string | null;
  sections: MenuSection[];
}

export interface JoinResponse {
  sessionId: string;
  sessionCode: string;
  tableNumber: string;
  currentOrder: OrderResponse | null;
}

export interface OrderResponse {
  id: string | null;
  tableNumber: string;
  orderNumber: number;
  status: OrderStatus;
  subtotal: string;
  items: OrderItemResponse[];
}

export interface OrderItemResponse {
  id: string;
  menuItemId: string;
  name: string;
  quantity: number;
  unitPrice: string;
  basePrice: string;
  subtotal: string;
  notes: string | null;
  addedBy: string | null;
  modifiers: SelectedModifierResponse[];
}

export interface SelectedModifierResponse {
  id: string;
  name: string;
  priceAdjustment: string;
  modifierType: string;
}

export type OrderStatus = 'DRAFT' | 'SUBMITTED' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'DELIVERED' | 'BILL_REQUESTED' | 'CANCELLED';

export type EventType = 'MENU_VIEW' | 'ITEM_VIEW' | 'SECTION_VIEW' | 'FILTER_USED';

export interface RecordEventRequest {
  eventType: EventType;
  itemId?: string;
  sectionId?: string;
  sessionId: string;
  metadata?: Record<string, string>;
}

export interface CartItem {
  menuItemId: string;
  name: string;
  price: string;
  quantity: number;
  notes?: string;
  guestName?: string;
  selectedModifiers?: string[];
}
