import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { formatNumber } from '@/lib/utils';
import type { ViewedVsSoldItem, SalesPeriod } from '@/shared/types';
import { salesPeriodLabel } from './salesPeriod';

interface ViewedVsSoldMatrixProps {
  data: ViewedVsSoldItem[];
  salesPeriod: SalesPeriod;
}

export function ViewedVsSoldMatrix({ data, salesPeriod }: ViewedVsSoldMatrixProps) {
  const salesLabel = salesPeriodLabel(salesPeriod);

  if (data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Mirado vs vendido</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-center text-muted-foreground py-8">Sin datos suficientes</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Mirado vs vendido</CardTitle>
        <CardDescription>
          Vistas acumuladas (histórico) vs unidades vendidas · {salesLabel}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Plato</TableHead>
              <TableHead className="text-right">Vistas</TableHead>
              <TableHead className="text-right">Vendidos</TableHead>
              <TableHead className="text-right">Ratio</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((row) => {
              const ratio = row.viewCount > 0 ? row.quantitySold / row.viewCount : 0;
              return (
                <TableRow key={row.itemId}>
                  <TableCell className="font-medium">{row.itemName}</TableCell>
                  <TableCell className="text-right">{formatNumber(row.viewCount)}</TableCell>
                  <TableCell className="text-right">{formatNumber(row.quantitySold)}</TableCell>
                  <TableCell className="text-right">{(ratio * 100).toFixed(0)}%</TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
