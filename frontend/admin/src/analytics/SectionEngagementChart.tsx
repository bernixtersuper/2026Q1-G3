import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatNumber } from '@/lib/utils';
import type { SectionAnalytics } from '@/shared/types';

interface SectionEngagementChartProps {
  data: SectionAnalytics[];
}

export function SectionEngagementChart({ data }: SectionEngagementChartProps) {
  if (data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Section Engagement</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-center text-muted-foreground py-8">
            No section views recorded yet
          </p>
        </CardContent>
      </Card>
    );
  }

  const chartData = data.map((d) => ({
    name: d.sectionName,
    views: d.viewCount,
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Section Engagement</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} layout="vertical" margin={{ left: 20 }}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" horizontal={false} />
              <XAxis type="number" tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
              <YAxis
                type="category"
                dataKey="name"
                tick={{ fontSize: 12 }}
                tickLine={false}
                axisLine={false}
                width={100}
              />
              <Tooltip
                formatter={(value: number) => [formatNumber(value), 'Views']}
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px',
                }}
              />
              <Bar dataKey="views" fill="#8b5cf6" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
