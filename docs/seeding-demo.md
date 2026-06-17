# Seeding de datos para demo

Cómo poblar un tenant para que **todos** los paneles de analytics se vean completos en una presentación, sin esperar tráfico real.

El dashboard lee de **dos** almacenes y por eso el seeding tiene **dos partes** (el orden importa):

| Parte | Qué puebla | Cómo |
|-------|------------|------|
| **1. Relacional (PostgreSQL)** | Pedidos, ingresos, ticket promedio, top vendidos, heatmaps de pedidos, tendencias y **Menu Insights** (se piden juntos, ingeniería de menú, extras) | Endpoint oculto `POST /api/admin/demo/seed` (corre dentro de la VPC; RDS es privada) |
| **2. Tráfico (DynamoDB)** | Vistas de menú, ítems vistos, sesiones únicas, uso de filtros, engagement por sección, realtime | Script `seed_analytics_dynamo.py` (escribe directo a Dynamo con **fechas simuladas**) |

> ¿Por qué un script para Dynamo y un endpoint para RDS? Los ítems de Dynamo llevan la fecha en la **sort key** (`DAY#2026-06-01`), así que cualquier día pasado se puede escribir directo. Los eventos de interacción, en cambio, se marcan siempre con la hora actual y no pueden backfillear histórico. Y RDS está en subredes privadas: solo se la alcanza desde dentro de la VPC, por eso lo relacional va por el endpoint del backend.

---

## 1. Requisitos previos

1. **Habilitar el endpoint oculto** (está deshabilitado por defecto; responde `404` mientras tanto). En `terraform/terraform.tfvars`:

   ```hcl
   demo_seed_enabled = true
   ```

   Luego `terraform apply` (setea `DEMO_SEED_ENABLED=true` en la task ECS y la redespliega).

2. **Credenciales AWS** activas (las del Learner Lab). Dependencias Python del script Dynamo (`boto3`):

   ```bash
   # seed-demo.sh crea .venv-seed en la raíz del repo si hace falta
   TOKEN=<jwt> bash terraform/scripts/seed-demo.sh

   # o manualmente:
   python3 -m venv .venv-seed
   .venv-seed/bin/pip install -r analytics-processor/scripts/requirements.txt
   .venv-seed/bin/python analytics-processor/scripts/seed_analytics_dynamo.py ...
   ```

   En AWS CloudShell `boto3` suele venir preinstalado (no hace falta venv).

3. El **JWT de admin** del tenant a poblar (ver abajo).

---

## 2. Cómo obtener el JWT de admin

El backend no emite tokens propios: valida el **access token de Cognito**. El SPA admin lo guarda en `localStorage` bajo la clave `md_token` después del login, y lo manda como `Authorization: Bearer …` en cada llamada a `/api/admin/*`. Ese mismo token es el que necesita el seeder.

**Opción A — desde el navegador (la más simple):**

1. Iniciá sesión en el panel admin (`frontend_admin_website_url`).
2. Abrí **DevTools → Console** y ejecutá:

   ```js
   localStorage.getItem('md_token')
   ```

3. Copiá el string (sin las comillas). Ese es tu `TOKEN`.

**Opción B — desde la pestaña Network:** en DevTools → **Network**, hacé clic en cualquier request a `/api/admin/...` → **Headers** → copiá el valor de `Authorization` después de `Bearer `.

> El token de Cognito expira (típicamente 1 h). Si el seed falla con `401`, volvé a copiarlo.

---

## 3. Seed completo (un solo comando)

```bash
TOKEN=<admin-jwt> bash terraform/scripts/seed-demo.sh
```

Corre las dos partes en orden (pedidos primero, así existe el menú cuyo IDs usa el paso de Dynamo). Resuelve la URL del backend con `terraform output`.

Opciones útiles:

```bash
# Más historia y URL explícita
TOKEN=<jwt> API_URL=http://mi-alb DAYS=45 bash terraform/scripts/seed-demo.sh

# Solo una de las partes
SKIP_DYNAMO=1 TOKEN=<jwt> bash terraform/scripts/seed-demo.sh   # solo pedidos (PostgreSQL)
SKIP_ORDERS=1 TOKEN=<jwt> bash terraform/scripts/seed-demo.sh   # solo vistas (DynamoDB)
```

---

## 4. Pasos individuales (equivalente manual)

**Parte 1 — relacional (pedidos / menú / mesas):**

```bash
curl -X POST "http://<alb-host>/api/admin/demo/seed?days=30" \
  -H "Authorization: Bearer $TOKEN"
# -> {"days":30,"menuItemsCreated":18,"tablesCreated":6,"ordersCreated":312,...}
```

**Parte 2 — tráfico (DynamoDB):**

```bash
python3 analytics-processor/scripts/seed_analytics_dynamo.py \
  --api-url http://<alb-host> --token "$TOKEN" --days 30
```

### Previsualizar sin escribir (`--dry-run`)

Imprime exactamente los ítems que escribiría, **sin tocar DynamoDB** (no requiere credenciales AWS):

```bash
python3 analytics-processor/scripts/seed_analytics_dynamo.py \
  --api-url http://<alb-host> --token "$TOKEN" --days 7 --dry-run
```

```
==> tenant 1111… | tz=UTC | days=7 | DRY-RUN (no writes)
  [dry-run] PUT DAY#2026-06-11  {'menuViews':'42','itemViews':'98',...,'uniqueMenuSessions':'30'} top=10
  [dry-run] PUT HOUR#2026-06-11T12  {'menuViews':'8','itemViews':'17','cartAdds':'5'}
  ...
Would seed DynamoDB analytics for tenant 1111…: 7 DAY#, 38 HOUR#, 18 ITEM# items (2026-06-11 → 2026-06-17).
```

### Zona horaria (`--tz`)

Las keys `DAY#`/`HOUR#` se calculan en UTC por defecto (igual que Fargate). Si el backend corriera en otra zona, alineá las fechas:

```bash
python3 analytics-processor/scripts/seed_analytics_dynamo.py \
  --api-url http://<alb-host> --token "$TOKEN" --tz America/Argentina/Buenos_Aires
```

Flags completos: `--table` (default `menuqr-analytics`), `--region` (default `us-east-1`), `--days` (1–90), `--top-n`, `--tz`, `--dry-run`.

---

## 5. Verificar

Refrescá el dashboard en el panel admin:

- **Resumen / Tendencias** → pedidos, ingresos, vistas, conversión **FINAL** (el script setea `batchCompletedAt`, no hace falta correr Glue).
- **Insights** → "Se piden juntos", ingeniería de menú (las 4 categorías), extras.
- **Operaciones** → heatmaps de pedidos y vistas.
- El panel **realtime** muestra actividad de la hora actual.

### «Top vistos» vacío pero hay vistas de menú

Son **dos métricas distintas** en Dynamo:

| Métrica | Ítem Dynamo | Panel |
|---------|-------------|--------|
| Vistas de menú | `DAY#.menuViews` | KPI «Vistas menú», gráfico Tendencias |
| Vistas por ítem | `ITEM#.views` (acumulado) + `DAY#.itemViews` | «Top vistos», línea Item Views |

El seed escribe **ambos**, pero el ranking «Top vistos» lee sobre todo `ITEM#`. Si solo corriste la parte relacional (`SKIP_DYNAMO=1`) o el paso 2 falló, verás pedidos/vistas de menú sin ranking por plato.

Comprobación rápida (reemplazá `TENANT_ID`):

```bash
aws dynamodb query --table-name menuqr-analytics \
  --key-condition-expression "PK = :pk AND begins_with(SK, :pfx)" \
  --expression-attribute-values '{":pk":{"S":"TENANT#TENANT_ID"},":pfx":{"S":"ITEM#"}}' \
  --select COUNT
```

Debería devolver tantos ítems como platos en el menú. Si `Count = 0`, re-ejecutá solo Dynamo:

```bash
TOKEN=<jwt> SKIP_ORDERS=1 bash terraform/scripts/seed-demo.sh
```

El script imprime al final cuántos `ITEM#` tienen `views>0`.

---

## 6. Notas

- **Idempotencia:** el menú se crea solo si el tenant tiene pocos ítems; los pedidos se **agregan** en cada corrida. Los ítems `DAY#/HOUR#/ITEM#` se **sobreescriben** (`PutItem`). Pensado para tenants de demo, no para uno con tráfico real que quieras conservar.
- **Sin TTL:** a diferencia de los registros `PROC#`, los ítems `DAY#/HOUR#/ITEM#` no llevan TTL, así que la historia simulada no expira.
- **Tenant:** el seed afecta **solo** el tenant del JWT. Para poblar otro, usá su token.
