import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { TrendingUp } from 'lucide-react';
import { formatNumber, formatPercent } from '@/lib/utils';
import { cn } from '@/lib/utils';
import type { ItemAnalytics } from '@/shared/types';

interface ItemRankingTableProps {
  data: ItemAnalytics[];
}

export function ItemRankingTable({ data }: ItemRankingTableProps) {
  if (data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Top Items</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-center text-muted-foreground py-8">
            No item views recorded yet
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Top Items</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-12">#</TableHead>
              <TableHead>Item</TableHead>
              <TableHead className="text-right">Views</TableHead>
              <TableHead className="text-right">View Rate</TableHead>
              <TableHead className="w-16"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((item, index) => (
              <TableRow
                key={item.itemId}
                className={cn(index < 3 && 'bg-purple-50/50')}
              >
                <TableCell className="font-medium">
                  <span
                    className={cn(
                      'inline-flex items-center justify-center w-6 h-6 rounded-full text-xs',
                      index === 0 && 'bg-yellow-100 text-yellow-700',
                      index === 1 && 'bg-slate-100 text-slate-700',
                      index === 2 && 'bg-amber-100 text-amber-700',
                      index > 2 && 'bg-muted text-muted-foreground'
                    )}
                  >
                    {index + 1}
                  </span>
                </TableCell>
                <TableCell className="font-medium">{item.itemName}</TableCell>
                <TableCell className="text-right">{formatNumber(item.viewCount)}</TableCell>
                <TableCell className="text-right">{formatPercent(item.viewRate)}</TableCell>
                <TableCell>
                  {item.trending && (
                    <TrendingUp className="h-4 w-4 text-green-500" />
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
