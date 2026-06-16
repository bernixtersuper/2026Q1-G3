import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ShoppingBag, DollarSign, Eye, Users } from 'lucide-react';
import { formatNumber } from '@/lib/utils';
import type { AnalyticsSummary } from '@/shared/types';

interface KpiCardsProps {
  data: AnalyticsSummary;
}

export function KpiCards({ data }: KpiCardsProps) {
  const cards = [
    {
      icon: ShoppingBag,
      label: 'Pedidos hoy',
      value: formatNumber(data.ordersToday),
      sub: data.ordersYesterday > 0 ? `Ayer: ${formatNumber(data.ordersYesterday)}` : undefined,
      color: 'text-emerald-600',
      bgColor: 'bg-emerald-100',
    },
    {
      icon: DollarSign,
      label: 'Ticket promedio',
      value: `$${Number(data.avgTicket).toFixed(2)}`,
      sub: `Ingresos hoy: $${Number(data.revenueToday).toFixed(2)}`,
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
    },
    {
      icon: Eye,
      label: 'Vistas menú hoy',
      value: formatNumber(data.menuViewsToday),
      sub: data.menuViewsYesterday > 0 ? `Ayer: ${formatNumber(data.menuViewsYesterday)}` : undefined,
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
    },
    {
      icon: Users,
      label: 'Mesas activas',
      value: formatNumber(data.activeTables),
      sub: data.peakHourToday != null ? `Pico hoy: ${data.peakHourToday}:00` : undefined,
      color: 'text-amber-600',
      bgColor: 'bg-amber-100',
    },
  ];

  return (
    <div className="space-y-3">
      {data.conversionStatus === 'PRELIMINARY' && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Badge variant="outline">Conversión preliminar</Badge>
          <span>{data.conversionNote ?? 'La tasa de conversión estará disponible tras el batch nocturno.'}</span>
        </div>
      )}
      {data.conversionRate != null && (
        <div className="text-sm">
          <Badge variant="secondary">Conversión: {(data.conversionRate * 100).toFixed(1)}%</Badge>
        </div>
      )}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map((card) => (
          <Card key={card.label}>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className={`p-3 rounded-lg ${card.bgColor}`}>
                  <card.icon className={`h-6 w-6 ${card.color}`} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">{card.label}</p>
                  <p className="text-2xl font-bold">{card.value}</p>
                  {card.sub && <p className="text-xs text-muted-foreground mt-1">{card.sub}</p>}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
