import { Card, CardContent } from '@/components/ui/card';
import { Eye, Users, Layers, TrendingUp } from 'lucide-react';
import { formatNumber } from '@/lib/utils';
import type { AnalyticsDashboard } from '@/shared/types';

interface KpiCardsProps {
  data: AnalyticsDashboard;
}

export function KpiCards({ data }: KpiCardsProps) {
  const cards = [
    {
      icon: Eye,
      label: 'Views (30d)',
      value: formatNumber(data.totalMenuViewsLast30Days),
      color: 'text-purple-500',
      bgColor: 'bg-purple-100',
    },
    {
      icon: TrendingUp,
      label: 'Views today',
      value: formatNumber(data.totalMenuViewsToday),
      color: 'text-teal-500',
      bgColor: 'bg-teal-100',
    },
    {
      icon: Users,
      label: 'Unique sessions',
      value: formatNumber(data.uniqueSessionsLast30Days),
      color: 'text-blue-500',
      bgColor: 'bg-blue-100',
    },
    {
      icon: Layers,
      label: 'Avg session depth',
      value: `${data.avgSessionDepth.toFixed(1)} items`,
      color: 'text-amber-500',
      bgColor: 'bg-amber-100',
    },
  ];

  return (
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
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
