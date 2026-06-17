import {
  ResponsiveContainer,
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  ZAxis,
  CartesianGrid,
  Tooltip,
  ReferenceLine,
} from 'recharts';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { formatCurrency } from '@/lib/utils';
import type { MenuEngineering, MenuEngineeringClass } from '@/shared/types';

interface MenuEngineeringChartProps {
  data: MenuEngineering;
}

const CLASS_META: Record<
  MenuEngineeringClass,
  { label: string; hint: string; color: string; badgeClass: string }
> = {
  STAR: {
    label: 'Estrella',
    hint: 'Popular y rentable — destacar y proteger',
    color: '#10b981',
    badgeClass: 'bg-emerald-100 text-emerald-800',
  },
  PLOWHORSE: {
    label: 'Caballo de tiro',
    hint: 'Popular pero poco rentable — revisar precio/costo',
    color: '#3b82f6',
    badgeClass: 'bg-blue-100 text-blue-800',
  },
  PUZZLE: {
    label: 'Incógnita',
    hint: 'Rentable pero poco vendido — dar más visibilidad',
    color: '#f59e0b',
    badgeClass: 'bg-amber-100 text-amber-800',
  },
  DOG: {
    label: 'Perro',
    hint: 'Poco popular y poco rentable — candidato a quitar',
    color: '#ef4444',
    badgeClass: 'bg-red-100 text-red-800',
  },
};

const CLASS_ORDER: MenuEngineeringClass[] = ['STAR', 'PLOWHORSE', 'PUZZLE', 'DOG'];

interface TooltipPayloadItem {
  payload: { itemName: string; quantitySold: number; revenue: number; classification: MenuEngineeringClass };
}

function ChartTooltip({ active, payload }: { active?: boolean; payload?: TooltipPayloadItem[] }) {
  if (!active || !payload?.length) return null;
  const p = payload[0].payload;
  return (
    <div className="rounded-lg border bg-card p-3 text-sm shadow-md">
      <p className="font-semibold">{p.itemName}</p>
      <p className="text-muted-foreground">{CLASS_META[p.classification].label}</p>
      <p>Unidades: {p.quantitySold}</p>
      <p>Ingresos: {formatCurrency(p.revenue)}</p>
    </div>
  );
}

export function MenuEngineeringChart({ data }: MenuEngineeringChartProps) {
  const byClass = CLASS_ORDER.map((cls) => ({
    cls,
    points: data.items
      .filter((i) => i.classification === cls)
      .map((i) => ({
        itemName: i.itemName,
        quantitySold: i.quantitySold,
        revenue: Number(i.revenue),
        classification: i.classification,
      })),
  }));

  const counts = Object.fromEntries(
    CLASS_ORDER.map((cls) => [cls, data.items.filter((i) => i.classification === cls).length])
  ) as Record<MenuEngineeringClass, number>;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Ingeniería de menú</CardTitle>
        <CardDescription>
          Cada plato según popularidad (unidades vendidas) y rentabilidad (ingresos), comparado con
          el promedio del menú. Las líneas marcan los promedios.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2">
          {CLASS_ORDER.map((cls) => (
            <div key={cls} className="flex items-center gap-1.5" title={CLASS_META[cls].hint}>
              <Badge className={CLASS_META[cls].badgeClass}>
                {CLASS_META[cls].label}: {counts[cls]}
              </Badge>
            </div>
          ))}
        </div>

        {data.items.length === 0 ? (
          <p className="text-center text-muted-foreground py-8">
            Sin ventas en el período seleccionado
          </p>
        ) : (
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <ScatterChart margin={{ top: 16, right: 24, bottom: 24, left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis
                  type="number"
                  dataKey="quantitySold"
                  name="Unidades"
                  tick={{ fontSize: 12 }}
                  label={{ value: 'Unidades vendidas', position: 'insideBottom', offset: -12, fontSize: 12 }}
                />
                <YAxis
                  type="number"
                  dataKey="revenue"
                  name="Ingresos"
                  tick={{ fontSize: 12 }}
                  label={{ value: 'Ingresos ($)', angle: -90, position: 'insideLeft', fontSize: 12 }}
                />
                <ZAxis range={[80, 80]} />
                <ReferenceLine x={data.avgQuantity} stroke="#94a3b8" strokeDasharray="4 4" />
                <ReferenceLine y={data.avgRevenue} stroke="#94a3b8" strokeDasharray="4 4" />
                <Tooltip content={<ChartTooltip />} cursor={{ strokeDasharray: '3 3' }} />
                {byClass.map(({ cls, points }) => (
                  <Scatter key={cls} name={CLASS_META[cls].label} data={points} fill={CLASS_META[cls].color} />
                ))}
              </ScatterChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
