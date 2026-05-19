import {
  ResponsiveContainer,
  ComposedChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatDate } from '@/lib/utils';
import type { DailyViewCount } from '@/shared/types';

interface DailyViewsChartProps {
  data: DailyViewCount[];
}

export function DailyViewsChart({ data }: DailyViewsChartProps) {
  const chartData = data.map((d) => ({
    ...d,
    dateFormatted: formatDate(d.date),
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Daily Views (Last 30 Days)</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
              <XAxis
                dataKey="dateFormatted"
                tick={{ fontSize: 12 }}
                tickLine={false}
                axisLine={false}
                interval="preserveStartEnd"
              />
              <YAxis
                tick={{ fontSize: 12 }}
                tickLine={false}
                axisLine={false}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px',
                }}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="menuViews"
                name="Menu Views"
                stroke="#8b5cf6"
                strokeWidth={2}
                dot={false}
              />
              <Line
                type="monotone"
                dataKey="itemViews"
                name="Item Views"
                stroke="#14b8a6"
                strokeWidth={2}
                dot={false}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
