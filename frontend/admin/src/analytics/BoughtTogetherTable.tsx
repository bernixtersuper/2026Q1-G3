import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { formatNumber, formatPercent } from '@/lib/utils';
import type { ItemPairRow } from '@/shared/types';

interface BoughtTogetherTableProps {
  data: ItemPairRow[];
}

function liftBadge(lift: number) {
  if (lift >= 2) return { label: 'Fuerte', className: 'bg-emerald-100 text-emerald-800' };
  if (lift >= 1) return { label: 'Positiva', className: 'bg-teal-100 text-teal-800' };
  return { label: 'Débil', className: 'bg-muted text-muted-foreground' };
}

export function BoughtTogetherTable({ data }: BoughtTogetherTableProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Se piden juntos</CardTitle>
        <CardDescription>
          Pares de platos que aparecen en el mismo pedido. El "lift" &gt; 1 indica una asociación
          real (se piden juntos más de lo esperado por azar) — ideal para combos y sugerencias.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <p className="text-center text-muted-foreground py-8">
            Sin pares suficientes en el período seleccionado
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Par de platos</TableHead>
                <TableHead className="text-right">Pedidos</TableHead>
                <TableHead className="text-right">% pedidos</TableHead>
                <TableHead className="text-right">Lift</TableHead>
                <TableHead className="text-right">Asociación</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((pair) => {
                const badge = liftBadge(pair.lift);
                return (
                  <TableRow key={`${pair.itemAId}-${pair.itemBId}`}>
                    <TableCell className="font-medium">
                      {pair.itemAName} <span className="text-muted-foreground">+</span>{' '}
                      {pair.itemBName}
                    </TableCell>
                    <TableCell className="text-right">{formatNumber(pair.coOccurrenceCount)}</TableCell>
                    <TableCell className="text-right">{formatPercent(pair.support)}</TableCell>
                    <TableCell className="text-right">{pair.lift.toFixed(2)}×</TableCell>
                    <TableCell className="text-right">
                      <Badge className={badge.className}>{badge.label}</Badge>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
