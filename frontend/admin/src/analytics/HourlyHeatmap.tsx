import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';

interface HourlyHeatmapProps {
  data: Record<string, Record<number, number>>;
  title?: string;
  description?: string;
  unitLabel?: string;
}

const dayOrder = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const dayLabels: Record<string, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
  SUNDAY: 'Sun',
};

const dayAliases: Record<string, string> = {
  Monday: 'MONDAY',
  Tuesday: 'TUESDAY',
  Wednesday: 'WEDNESDAY',
  Thursday: 'THURSDAY',
  Friday: 'FRIDAY',
  Saturday: 'SATURDAY',
  Sunday: 'SUNDAY',
};

function normalizeDayKey(day: string): string {
  return dayAliases[day] ?? day.toUpperCase();
}

function getColor(value: number, max: number): string {
  if (max === 0) return 'bg-slate-50';
  const intensity = value / max;

  if (intensity === 0) return 'bg-slate-50';
  if (intensity < 0.2) return 'bg-purple-100';
  if (intensity < 0.4) return 'bg-purple-200';
  if (intensity < 0.6) return 'bg-purple-300';
  if (intensity < 0.8) return 'bg-purple-400';
  return 'bg-purple-500';
}

export function normalizeHeatmapDays(
  data: Record<string, Record<number, number>>
): Record<string, Record<number, number>> {
  const normalized: Record<string, Record<number, number>> = {};
  Object.entries(data).forEach(([day, hours]) => {
    normalized[normalizeDayKey(day)] = hours;
  });
  return normalized;
}

export function HourlyHeatmap({
  data,
  title = 'Activity Heatmap',
  description = 'Activity by hour of day and day of week',
  unitLabel = 'events',
}: HourlyHeatmapProps) {
  const normalized = normalizeHeatmapDays(data);
  const hours = Array.from({ length: 24 }, (_, i) => i);

  let maxValue = 0;
  dayOrder.forEach((day) => {
    if (normalized[day]) {
      Object.values(normalized[day]).forEach((v) => {
        if (v > maxValue) maxValue = v;
      });
    }
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="overflow-x-auto">
          <div className="min-w-[600px]">
            <div className="flex">
              <div className="w-16" />
              <div className="flex-1 flex">
                {hours.map((hour) => (
                  <div
                    key={hour}
                    className="flex-1 text-center text-xs text-muted-foreground pb-2"
                  >
                    {hour % 3 === 0 ? `${hour}:00` : ''}
                  </div>
                ))}
              </div>
            </div>
            {dayOrder.map((day) => (
              <div key={day} className="flex items-center">
                <div className="w-16 text-sm font-medium pr-2 text-right">
                  {dayLabels[day]}
                </div>
                <div className="flex-1 flex gap-0.5">
                  {hours.map((hour) => {
                    const value = normalized[day]?.[hour] || 0;
                    return (
                      <div
                        key={hour}
                        className={`flex-1 h-8 rounded-sm transition-colors ${getColor(value, maxValue)}`}
                        title={`${dayLabels[day]} ${hour}:00 - ${value} ${unitLabel}`}
                      />
                    );
                  })}
                </div>
              </div>
            ))}
            <div className="flex items-center justify-end mt-4 gap-2">
              <span className="text-xs text-muted-foreground">Less</span>
              <div className="flex gap-0.5">
                {['bg-slate-50', 'bg-purple-100', 'bg-purple-200', 'bg-purple-300', 'bg-purple-400', 'bg-purple-500'].map((color, i) => (
                  <div key={i} className={`w-4 h-4 rounded-sm ${color}`} />
                ))}
              </div>
              <span className="text-xs text-muted-foreground">More</span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
