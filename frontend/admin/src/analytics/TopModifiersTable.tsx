import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { formatCurrency, formatNumber } from '@/lib/utils';
import type { ModifierRow } from '@/shared/types';

interface TopModifiersTableProps {
  data: ModifierRow[];
}

export function TopModifiersTable({ data }: TopModifiersTableProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Extras más pedidos</CardTitle>
        <CardDescription>
          Modificadores (extras, agregados) más seleccionados y los ingresos adicionales que generan.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {data.length === 0 ? (
          <p className="text-center text-muted-foreground py-8">Sin extras registrados en el período</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-12">#</TableHead>
                <TableHead>Extra</TableHead>
                <TableHead className="text-right">Veces pedido</TableHead>
                <TableHead className="text-right">Ingresos extra</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((mod, index) => (
                <TableRow key={mod.modifierName}>
                  <TableCell className="font-medium">{index + 1}</TableCell>
                  <TableCell className="font-medium">{mod.modifierName}</TableCell>
                  <TableCell className="text-right">{formatNumber(mod.timesSelected)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(mod.revenue)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
