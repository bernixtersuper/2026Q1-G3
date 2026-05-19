import { useState, useRef, useCallback, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { tableApi, type Table, type TablePositionUpdate } from '../shared/api/adminApi';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Users, Move, Save, RotateCcw } from 'lucide-react';
import { toast } from '../hooks/use-toast';

interface FloorPlanViewProps {
  tables: Table[];
  onTableClick: (table: Table) => void;
}

const GRID_SIZE = 20;
const MIN_TABLE_SIZE = 60;
const DEFAULT_WIDTH = 100;
const DEFAULT_HEIGHT = 80;

function snapToGrid(value: number): number {
  return Math.round(value / GRID_SIZE) * GRID_SIZE;
}

function getTableStatus(table: Table): { color: string; label: string; bgColor: string } {
  if (!table.active) {
    return { color: '#9ca3af', label: 'Inactive', bgColor: '#f3f4f6' };
  }
  if (!table.hasActiveSession) {
    return { color: '#22c55e', label: 'Available', bgColor: '#dcfce7' };
  }
  if (table.sessionRemainingMinutes <= 15) {
    return { color: '#f97316', label: 'Expiring', bgColor: '#fed7aa' };
  }
  return { color: '#3b82f6', label: 'Occupied', bgColor: '#dbeafe' };
}

export function FloorPlanView({ tables, onTableClick }: FloorPlanViewProps) {
  const queryClient = useQueryClient();
  const containerRef = useRef<HTMLDivElement>(null);
  const [positions, setPositions] = useState<Map<string, { x: number; y: number; width: number; height: number }>>(new Map());
  const [dragging, setDragging] = useState<string | null>(null);
  const [resizing, setResizing] = useState<string | null>(null);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [hasChanges, setHasChanges] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);

  useEffect(() => {
    const newPositions = new Map<string, { x: number; y: number; width: number; height: number }>();
    tables.forEach((table, index) => {
      newPositions.set(table.id, {
        x: table.positionX ?? (50 + (index % 4) * 130),
        y: table.positionY ?? (50 + Math.floor(index / 4) * 110),
        width: table.width ?? DEFAULT_WIDTH,
        height: table.height ?? DEFAULT_HEIGHT,
      });
    });
    setPositions(newPositions);
    setHasChanges(false);
  }, [tables]);

  const savePositionsMutation = useMutation({
    mutationFn: async () => {
      const updates: TablePositionUpdate[] = [];
      positions.forEach((pos, id) => {
        updates.push({
          id,
          positionX: pos.x,
          positionY: pos.y,
          width: pos.width,
          height: pos.height,
        });
      });
      return tableApi.updatePositions(updates);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tables'] });
      setHasChanges(false);
      setIsEditMode(false);
      toast({ title: 'Layout Saved', description: 'Floor plan has been saved', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to save layout', variant: 'destructive' }),
  });

  const handleMouseDown = useCallback((e: React.MouseEvent, tableId: string, isResize: boolean = false) => {
    if (!isEditMode) return;
    e.stopPropagation();
    
    const pos = positions.get(tableId);
    if (!pos) return;
    
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    
    if (isResize) {
      setResizing(tableId);
    } else {
      setDragging(tableId);
      setDragOffset({
        x: e.clientX - rect.left - pos.x,
        y: e.clientY - rect.top - pos.y,
      });
    }
  }, [isEditMode, positions]);

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!dragging && !resizing) return;
    
    const rect = containerRef.current?.getBoundingClientRect();
    if (!rect) return;
    
    const targetId = dragging || resizing;
    if (!targetId) return;
    
    const pos = positions.get(targetId);
    if (!pos) return;
    
    if (dragging) {
      const newX = snapToGrid(Math.max(0, e.clientX - rect.left - dragOffset.x));
      const newY = snapToGrid(Math.max(0, e.clientY - rect.top - dragOffset.y));
      
      setPositions(prev => new Map(prev).set(targetId, { ...pos, x: newX, y: newY }));
      setHasChanges(true);
    } else if (resizing) {
      const newWidth = snapToGrid(Math.max(MIN_TABLE_SIZE, e.clientX - rect.left - pos.x));
      const newHeight = snapToGrid(Math.max(MIN_TABLE_SIZE, e.clientY - rect.top - pos.y));
      
      setPositions(prev => new Map(prev).set(targetId, { ...pos, width: newWidth, height: newHeight }));
      setHasChanges(true);
    }
  }, [dragging, resizing, dragOffset, positions]);

  const handleMouseUp = useCallback(() => {
    setDragging(null);
    setResizing(null);
  }, []);

  const resetLayout = () => {
    const newPositions = new Map<string, { x: number; y: number; width: number; height: number }>();
    tables.forEach((table, index) => {
      newPositions.set(table.id, {
        x: 50 + (index % 4) * 130,
        y: 50 + Math.floor(index / 4) * 110,
        width: DEFAULT_WIDTH,
        height: DEFAULT_HEIGHT,
      });
    });
    setPositions(newPositions);
    setHasChanges(true);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <h2 className="text-lg font-semibold">Floor Plan</h2>
          <div className="flex items-center gap-2 text-sm">
            <span className="flex items-center gap-1">
              <span className="h-3 w-3 rounded-full bg-green-500" /> Available
            </span>
            <span className="flex items-center gap-1">
              <span className="h-3 w-3 rounded-full bg-blue-500" /> Occupied
            </span>
            <span className="flex items-center gap-1">
              <span className="h-3 w-3 rounded-full bg-orange-500" /> Expiring
            </span>
            <span className="flex items-center gap-1">
              <span className="h-3 w-3 rounded-full bg-gray-400" /> Inactive
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {isEditMode ? (
            <>
              <Button variant="outline" size="sm" onClick={resetLayout}>
                <RotateCcw className="mr-2 h-4 w-4" />
                Reset
              </Button>
              <Button variant="outline" size="sm" onClick={() => { setIsEditMode(false); setHasChanges(false); }}>
                Cancel
              </Button>
              <Button 
                size="sm" 
                onClick={() => savePositionsMutation.mutate()}
                disabled={!hasChanges || savePositionsMutation.isPending}
              >
                <Save className="mr-2 h-4 w-4" />
                {savePositionsMutation.isPending ? 'Saving...' : 'Save Layout'}
              </Button>
            </>
          ) : (
            <Button variant="outline" size="sm" onClick={() => setIsEditMode(true)}>
              <Move className="mr-2 h-4 w-4" />
              Edit Layout
            </Button>
          )}
        </div>
      </div>

      <div 
        ref={containerRef}
        className={`relative min-h-[500px] rounded-lg border-2 border-dashed bg-muted/30 ${isEditMode ? 'cursor-crosshair border-primary/50' : ''}`}
        style={{
          backgroundImage: isEditMode ? 
            `linear-gradient(to right, hsl(var(--border)) 1px, transparent 1px),
             linear-gradient(to bottom, hsl(var(--border)) 1px, transparent 1px)` : 'none',
          backgroundSize: `${GRID_SIZE}px ${GRID_SIZE}px`,
        }}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
      >
        {tables.map(table => {
          const pos = positions.get(table.id);
          if (!pos) return null;
          
          const status = getTableStatus(table);
          const isDragging = dragging === table.id;
          const isResizingThis = resizing === table.id;
          
          return (
            <div
              key={table.id}
              className={`absolute flex flex-col items-center justify-center rounded-lg border-2 shadow-sm transition-shadow ${
                isDragging || isResizingThis ? 'z-50 shadow-lg' : 'z-10'
              } ${isEditMode ? 'cursor-move' : 'cursor-pointer hover:shadow-md'}`}
              style={{
                left: pos.x,
                top: pos.y,
                width: pos.width,
                height: pos.height,
                backgroundColor: status.bgColor,
                borderColor: status.color,
              }}
              onMouseDown={(e) => !isEditMode ? onTableClick(table) : handleMouseDown(e, table.id)}
              onClick={() => !isEditMode && onTableClick(table)}
            >
              <span className="text-lg font-bold" style={{ color: status.color }}>
                {table.tableNumber}
              </span>
              {table.tableName && (
                <span className="text-xs text-muted-foreground truncate max-w-full px-1">
                  {table.tableName}
                </span>
              )}
              
              <div className="mt-1 flex items-center gap-1 text-xs" style={{ color: status.color }}>
                <Users className="h-3 w-3" />
                <span>{table.capacity}</span>
              </div>
              
              {table.hasActiveSession && (
                <Badge 
                  variant="outline" 
                  className="mt-1 text-[10px] px-1 py-0"
                  style={{ borderColor: status.color, color: status.color }}
                >
                  {table.sessionRemainingMinutes <= 60 
                    ? `${table.sessionRemainingMinutes}m` 
                    : `${Math.floor(table.sessionRemainingMinutes / 60)}h`}
                </Badge>
              )}
              
              {isEditMode && (
                <div
                  className="absolute -bottom-1 -right-1 h-4 w-4 cursor-se-resize rounded-sm bg-primary"
                  onMouseDown={(e) => handleMouseDown(e, table.id, true)}
                />
              )}
            </div>
          );
        })}
        
        {tables.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center text-muted-foreground">
            No tables yet. Add tables to see them on the floor plan.
          </div>
        )}
      </div>
    </div>
  );
}
