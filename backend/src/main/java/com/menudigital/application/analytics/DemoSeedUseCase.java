package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.DietaryTag;
import com.menudigital.domain.menu.Menu;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuItemModifier;
import com.menudigital.domain.menu.MenuSection;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.menu.ModifierType;
import com.menudigital.domain.order.Order;
import com.menudigital.domain.order.OrderItem;
import com.menudigital.domain.order.OrderRepository;
import com.menudigital.domain.order.OrderStatus;
import com.menudigital.domain.order.SelectedModifier;
import com.menudigital.domain.table.RestaurantTable;
import com.menudigital.domain.table.TableRepository;
import com.menudigital.domain.table.TableSession;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Populates the current tenant with a realistic, fully-featured dataset for a demo:
 * a menu (sections, items, modifiers), tables/sessions, and a backdated order history.
 * Menu items are deliberately spread across price and popularity so every menu-engineering
 * quadrant appears, and a few item affinities make the "frequently bought together" panel
 * meaningful.
 *
 * This seeds only the relational (PostgreSQL) side — orders, revenue and menu insights. The
 * DynamoDB views/traffic side is seeded separately by analytics-processor/scripts/seed_analytics_dynamo.py
 * (direct writes with simulated dates), since interaction events cannot be backdated.
 *
 * Idempotent-ish: the menu is created only if the tenant has few items; orders are always
 * appended. Uses a fixed RNG seed so repeated demos look consistent.
 */
@ApplicationScoped
public class DemoSeedUseCase {

    private static final Logger LOG = Logger.getLogger(DemoSeedUseCase.class);
    private static final long SEED = 42L;
    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 90;

    @Inject
    MenuRepository menuRepository;

    @Inject
    TableRepository tableRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    TenantContext tenantContext;

    public record DemoSeedResponse(
        int days,
        int menuItemsCreated,
        int tablesCreated,
        int ordersCreated,
        int orderItemsCreated,
        int modifiersAttached,
        String fromDate,
        String toDate
    ) {}

    /** A menu item enriched with the demo weights/affinities the generator needs. */
    private record SeedItem(
        UUID id, String name, BigDecimal price, UUID sectionId,
        int popularityWeight, List<SeedModifier> modifiers
    ) {}

    private record SeedModifier(UUID id, String name, BigDecimal priceAdjustment, String type) {}

    @Transactional
    public DemoSeedResponse execute(Integer requestedDays) {
        TenantId tenantId = tenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant in context");
        }
        int days = requestedDays == null ? DEFAULT_DAYS : Math.max(1, Math.min(MAX_DAYS, requestedDays));
        Random rnd = new Random(SEED);
        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zone);

        MenuBuild menu = ensureMenu(tenantId, rnd);
        List<TableRef> tables = ensureTables(tenantId);

        // --- Backdated order history -------------------------------------------------
        int orderNumber = 1000;
        int ordersCreated = 0, orderItemsCreated = 0, modifiersAttached = 0;

        for (int d = days - 1; d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            boolean weekend = day.getDayOfWeek().getValue() >= 6;
            int ordersToday = (weekend ? 10 : 6) + rnd.nextInt(weekend ? 8 : 6);

            for (int i = 0; i < ordersToday; i++) {
                Instant ts = randomServiceInstant(day, zone, now, rnd);
                TableRef table = tables.get(rnd.nextInt(tables.size()));
                boolean delivered = rnd.nextDouble() < 0.9;

                List<SeedItem> basket = pickBasket(menu.items, rnd);
                BigDecimal subtotal = BigDecimal.ZERO;

                Order order = new Order(
                    UUID.randomUUID(), tenantId, table.tableId, table.sessionId, table.tableNumber,
                    orderNumber++, delivered ? OrderStatus.DELIVERED : OrderStatus.SUBMITTED,
                    null, BigDecimal.ZERO, ts, ts, ts, delivered ? ts : null
                );
                orderRepository.save(order);

                for (SeedItem item : basket) {
                    int qty = 1 + rnd.nextInt(2);
                    SeedModifier mod = (!item.modifiers.isEmpty() && rnd.nextDouble() < 0.4)
                        ? item.modifiers.get(rnd.nextInt(item.modifiers.size()))
                        : null;
                    BigDecimal modTotal = mod != null ? mod.priceAdjustment : BigDecimal.ZERO;
                    BigDecimal unitPrice = item.price.add(modTotal);
                    subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(qty)));

                    UUID orderItemId = UUID.randomUUID();
                    OrderItem oi = new OrderItem(
                        orderItemId, order.getId(), item.id, item.name,
                        qty, unitPrice, item.price, null, "demo-seed", ts
                    );
                    orderRepository.saveItem(oi);
                    orderItemsCreated++;

                    if (mod != null) {
                        SelectedModifier sm = new SelectedModifier(
                            UUID.randomUUID(), orderItemId, mod.id, mod.name, mod.priceAdjustment, mod.type, ts
                        );
                        orderRepository.saveItemModifiers(orderItemId, List.of(sm));
                        modifiersAttached++;
                    }
                }

                order.setSubtotal(subtotal);
                orderRepository.update(order);
                ordersCreated++;
            }
        }

        LOG.infof("Demo seed for tenant %s: %d orders, %d items over %d days",
            tenantId, ordersCreated, orderItemsCreated, days);

        return new DemoSeedResponse(
            days, menu.created, tables.size(), ordersCreated, orderItemsCreated,
            modifiersAttached,
            today.minusDays(days - 1L).toString(), today.toString()
        );
    }

    // --- Menu ----------------------------------------------------------------------

    private record MenuBuild(List<SeedItem> items, int created) {}

    private MenuBuild ensureMenu(TenantId tenantId, Random rnd) {
        Menu existing = menuRepository.findByTenantId(tenantId);
        int existingItems = existing.getSections().stream().mapToInt(s -> s.getItems().size()).sum();
        if (existingItems >= 6) {
            return new MenuBuild(reuseMenu(existing), 0);
        }
        return createDemoMenu(tenantId);
    }

    private List<SeedItem> reuseMenu(Menu menu) {
        List<SeedItem> items = new ArrayList<>();
        int weight = 10;
        for (MenuSection section : menu.getSections()) {
            for (MenuItem item : section.getItems()) {
                List<SeedModifier> mods = new ArrayList<>();
                for (MenuItemModifier m : menuRepository.findModifiersByItemId(item.getId())) {
                    mods.add(new SeedModifier(m.getId(), m.getName(), m.getPriceAdjustment(), m.getModifierType().name()));
                }
                items.add(new SeedItem(item.getId(), item.getName(), item.getPrice(), section.getId(),
                    Math.max(1, weight), mods));
                weight = weight > 1 ? weight - 1 : 1;
            }
        }
        return items;
    }

    /** Hand-crafted menu tuned so popularity and price vary independently (all quadrants). */
    private MenuBuild createDemoMenu(TenantId tenantId) {
        // {section, name, price, popularityWeight, modifiers...}
        Object[][] spec = {
            {"Entradas", "Empanada de carne", "3.50", 9, new String[][]{}},
            {"Entradas", "Tabla de quesos", "12.00", 3, new String[][]{}},
            {"Entradas", "Rabas", "9.50", 5, new String[][]{}},
            {"Principales", "Milanesa napolitana", "11.00", 10, new String[][]{{"Doble carne", "3.00", "EXTRA"}}},
            {"Principales", "Bife de chorizo", "16.50", 6, new String[][]{{"Punto jugoso", "0.00", "SUBSTITUTION"}}},
            {"Principales", "Risotto de hongos", "13.00", 2, new String[][]{}},
            {"Pizzas", "Pizza muzzarella", "9.00", 10, new String[][]{{"Extra queso", "1.50", "EXTRA"}}},
            {"Pizzas", "Pizza napolitana", "10.50", 7, new String[][]{{"Extra queso", "1.50", "EXTRA"}}},
            {"Pizzas", "Pizza fugazzeta", "11.00", 4, new String[][]{}},
            {"Hamburguesas", "Hamburguesa clásica", "10.00", 9, new String[][]{{"Doble medallón", "3.50", "EXTRA"}, {"Sin cebolla", "0.00", "REMOVAL"}}},
            {"Hamburguesas", "Papas fritas", "5.00", 11, new String[][]{{"Cheddar y panceta", "2.50", "EXTRA"}}},
            {"Bebidas", "Gaseosa", "3.00", 12, new String[][]{{"Grande", "1.50", "SIZE"}}},
            {"Bebidas", "Agua mineral", "2.50", 6, new String[][]{}},
            {"Bebidas", "Cerveza artesanal", "6.50", 5, new String[][]{{"Pinta", "2.00", "SIZE"}}},
            {"Bebidas", "Copa de vino", "7.00", 3, new String[][]{}},
            {"Postres", "Flan casero", "5.50", 6, new String[][]{{"Con dulce de leche", "1.00", "EXTRA"}}},
            {"Postres", "Helado", "4.50", 7, new String[][]{}},
            {"Postres", "Brownie", "6.00", 4, new String[][]{}},
        };

        Map<String, UUID> sectionIds = new LinkedHashMap<>();
        Map<String, Set<DietaryTag>> tagsByItem = Map.of(
            "Risotto de hongos", Set.of(DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE),
            "Agua mineral", Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE),
            "Helado", Set.of(DietaryTag.VEGETARIAN),
            "Pizza muzzarella", Set.of(DietaryTag.VEGETARIAN)
        );

        List<SeedItem> items = new ArrayList<>();
        int displayOrder = 0;
        for (Object[] row : spec) {
            String sectionName = (String) row[0];
            String itemName = (String) row[1];
            BigDecimal price = new BigDecimal((String) row[2]);
            int weight = (Integer) row[3];
            String[][] modSpecs = (String[][]) row[4];

            UUID sectionId = sectionIds.get(sectionName);
            if (sectionId == null) {
                MenuSection s = menuRepository.saveSection(
                    MenuSection.create(tenantId, sectionName, sectionIds.size()));
                sectionId = s.getId();
                sectionIds.put(sectionName, sectionId);
            }

            MenuItem item = MenuItem.create(
                sectionId, tenantId, itemName, itemName,
                price, null, tagsByItem.getOrDefault(itemName, Set.of()), displayOrder++
            );
            menuRepository.saveItem(item);

            List<SeedModifier> mods = new ArrayList<>();
            int modOrder = 0;
            for (String[] m : modSpecs) {
                MenuItemModifier modifier = MenuItemModifier.create(
                    item.getId(), tenantId.value(), m[0], new BigDecimal(m[1]),
                    ModifierType.valueOf(m[2]), modOrder++
                );
                menuRepository.saveModifier(modifier);
                mods.add(new SeedModifier(modifier.getId(), modifier.getName(), modifier.getPriceAdjustment(), m[2]));
            }

            items.add(new SeedItem(item.getId(), itemName, price, sectionId, weight, mods));
        }

        return new MenuBuild(items, items.size());
    }

    // --- Tables --------------------------------------------------------------------

    private record TableRef(UUID tableId, String tableNumber, UUID sessionId) {}

    private List<TableRef> ensureTables(TenantId tenantId) {
        List<RestaurantTable> existing = tableRepository.findByTenantId(tenantId);
        List<TableRef> refs = new ArrayList<>();

        if (existing.isEmpty()) {
            for (int i = 1; i <= 6; i++) {
                RestaurantTable t = tableRepository.save(
                    RestaurantTable.create(tenantId, String.valueOf(i), "Mesa " + i, 4));
                TableSession s = tableRepository.saveSession(
                    TableSession.create(t.getId(), tenantId, "demo-seed"));
                refs.add(new TableRef(t.getId(), t.getTableNumber(), s.getId()));
            }
        } else {
            for (RestaurantTable t : existing) {
                UUID sessionId = tableRepository.findActiveSessionByTableId(t.getId())
                    .map(TableSession::getId)
                    .orElseGet(() -> tableRepository.saveSession(
                        TableSession.create(t.getId(), tenantId, "demo-seed")).getId());
                refs.add(new TableRef(t.getId(), t.getTableNumber(), sessionId));
            }
        }
        return refs;
    }

    // --- Order basket generation ---------------------------------------------------

    /**
     * Builds a basket of 1–4 distinct items. The primary item is popularity-weighted; an
     * affinity partner (mains↔drinks, burger↔fries, dessert) is frequently added so the
     * "bought together" panel surfaces real associations.
     */
    private List<SeedItem> pickBasket(List<SeedItem> items, Random rnd) {
        List<SeedItem> basket = new ArrayList<>();
        SeedItem primary = weightedPick(items, rnd);
        basket.add(primary);

        // Most table orders include a drink.
        if (rnd.nextDouble() < 0.75) {
            addDistinct(basket, pickFromSection(items, "Bebidas", rnd));
        }
        // Burgers very often come with fries.
        if (primary.name.startsWith("Hamburguesa") && rnd.nextDouble() < 0.7) {
            addDistinct(basket, findByName(items, "Papas fritas"));
        }
        // A second main/share dish sometimes.
        if (rnd.nextDouble() < 0.45) {
            addDistinct(basket, weightedPick(items, rnd));
        }
        // Dessert to finish.
        if (rnd.nextDouble() < 0.3) {
            addDistinct(basket, pickFromSection(items, "Postres", rnd));
        }
        return basket;
    }

    private void addDistinct(List<SeedItem> basket, SeedItem candidate) {
        if (candidate != null && basket.stream().noneMatch(b -> b.id.equals(candidate.id)) && basket.size() < 4) {
            basket.add(candidate);
        }
    }

    private SeedItem weightedPick(List<SeedItem> items, Random rnd) {
        int total = items.stream().mapToInt(SeedItem::popularityWeight).sum();
        int r = rnd.nextInt(total);
        int acc = 0;
        for (SeedItem item : items) {
            acc += item.popularityWeight;
            if (r < acc) return item;
        }
        return items.get(items.size() - 1);
    }

    private SeedItem pickFromSection(List<SeedItem> items, String sectionFirstItemHint, Random rnd) {
        // Section ids are opaque; match by the curated set of items belonging to known sections by name.
        List<SeedItem> pool = items.stream()
            .filter(i -> sectionOf(i).equals(sectionFirstItemHint))
            .toList();
        if (pool.isEmpty()) return null;
        return weightedPick(pool, rnd);
    }

    private SeedItem findByName(List<SeedItem> items, String name) {
        return items.stream().filter(i -> i.name.equals(name)).findFirst().orElse(null);
    }

    private String sectionOf(SeedItem item) {
        // Heuristic section grouping for the curated demo menu / reused menus.
        String n = item.name.toLowerCase();
        if (n.contains("gaseosa") || n.contains("agua") || n.contains("cerveza") || n.contains("vino")) return "Bebidas";
        if (n.contains("flan") || n.contains("helado") || n.contains("brownie")) return "Postres";
        return "Otros";
    }

    private Instant randomServiceInstant(LocalDate day, ZoneId zone, Instant now, Random rnd) {
        long dayStart = day.atStartOfDay(zone).toEpochSecond();
        int hour = 11 + rnd.nextInt(12); // 11:00–22:59 service window
        int minute = rnd.nextInt(60);
        Instant ts = Instant.ofEpochSecond(dayStart + hour * 3600L + minute * 60L);
        return ts.isAfter(now) ? now.minusSeconds(60 + rnd.nextInt(3600)) : ts;
    }
}
