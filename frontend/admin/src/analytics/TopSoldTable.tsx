import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { formatNumber } from '@/lib/utils';
import { cn } from '@/lib/utils';
import type { TopSoldItem, SalesPeriod } from '@/shared/types';
import { salesPeriodLabel } from './salesPeriod';

interface TopSoldTableProps {
  data: TopSoldItem[];
  period: SalesPeriod;
}

export function TopSoldTable({ data, period }: TopSoldTableProps) {
  const periodLabel = salesPeriodLabel(period);

  if (data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Top vendidos · {periodLabel}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-center text-muted-foreground py-8">
            Sin pedidos en el periodo seleccionado
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Top vendidos · {periodLabel}</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-12">#</TableHead>
              <TableHead>Plato</TableHead>
              <TableHead className="text-right">Unidades</TableHead>
              <TableHead className="text-right">Ingresos</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((item, index) => (
              <TableRow key={item.itemId} className={cn(index < 3 && 'bg-emerald-50/50')}>
                <TableCell className="font-medium">{index + 1}</TableCell>
                <TableCell className="font-medium">{item.itemName}</TableCell>
                <TableCell className="text-right">{formatNumber(item.quantitySold)}</TableCell>
                <TableCell className="text-right">${Number(item.revenue).toFixed(2)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
