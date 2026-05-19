import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatNumber, formatPercent } from '@/lib/utils';

interface FilterUsageChartProps {
  data: Record<string, number>;
}

const COLORS: Record<string, string> = {
  VEGAN: '#14b8a6',
  VEGETARIAN: '#22c55e',
  GLUTEN_FREE: '#f59e0b',
  DAIRY_FREE: '#8b5cf6',
};

const LABELS: Record<string, string> = {
  VEGAN: 'Vegan',
  VEGETARIAN: 'Vegetarian',
  GLUTEN_FREE: 'Gluten-free',
  DAIRY_FREE: 'Dairy-free',
};

export function FilterUsageChart({ data }: FilterUsageChartProps) {
  const chartData = Object.entries(data).map(([name, value]) => ({
    name: LABELS[name] || name,
    value,
    color: COLORS[name] || '#94a3b8',
  }));

  const total = chartData.reduce((sum, d) => sum + d.value, 0);

  if (chartData.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Filter Usage</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-center text-muted-foreground py-8">
            No filter usage recorded yet
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Filter Usage</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={80}
                paddingAngle={2}
                dataKey="value"
              >
                {chartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                formatter={(value: number) => [formatNumber(value), 'Uses']}
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px',
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
        <div className="mt-4 space-y-2">
          {chartData.map((item) => (
            <div key={item.name} className="flex items-center justify-between text-sm">
              <div className="flex items-center gap-2">
                <div
                  className="w-3 h-3 rounded-full"
                  style={{ backgroundColor: item.color }}
                />
                <span>{item.name}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-muted-foreground">
                  {formatNumber(item.value)}
                </span>
                <span className="w-16 text-right font-medium">
                  {formatPercent(item.value / total)}
                </span>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
