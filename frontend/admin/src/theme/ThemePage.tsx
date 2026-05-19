import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { themeApi, type Theme } from '../shared/api/adminApi';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Switch } from '../components/ui/switch';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Palette, RotateCcw, Save } from 'lucide-react';
import { toast } from '../hooks/use-toast';

const fontOptions = [
  'Inter',
  'Poppins',
  'Roboto',
  'Open Sans',
  'Lato',
  'Montserrat',
  'Playfair Display',
  'Merriweather',
];

const radiusOptions = [
  { value: 'none', label: 'None' },
  { value: 'sm', label: 'Small' },
  { value: 'md', label: 'Medium' },
  { value: 'lg', label: 'Large' },
  { value: 'xl', label: 'Extra Large' },
  { value: '2xl', label: '2X Large' },
  { value: 'full', label: 'Full' },
];

const gradientDirections = [
  { value: 'to-r', label: 'Left to Right' },
  { value: 'to-l', label: 'Right to Left' },
  { value: 'to-b', label: 'Top to Bottom' },
  { value: 'to-t', label: 'Bottom to Top' },
  { value: 'to-br', label: 'Top-Left to Bottom-Right' },
  { value: 'to-bl', label: 'Top-Right to Bottom-Left' },
];

export function ThemePage() {
  const queryClient = useQueryClient();
  const [localTheme, setLocalTheme] = useState<Theme | null>(null);

  const { data: theme, isLoading } = useQuery({
    queryKey: ['theme'],
    queryFn: themeApi.get,
  });

  useEffect(() => {
    if (theme && !localTheme) {
      setLocalTheme(theme);
    }
  }, [theme, localTheme]);

  const updateMutation = useMutation({
    mutationFn: themeApi.update,
    onSuccess: (data) => {
      queryClient.setQueryData(['theme'], data);
      setLocalTheme(data);
      toast({ title: 'Theme Saved', description: 'Your changes have been saved', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to save theme', variant: 'destructive' }),
  });

  const resetMutation = useMutation({
    mutationFn: themeApi.reset,
    onSuccess: (data) => {
      queryClient.setQueryData(['theme'], data);
      setLocalTheme(data);
      toast({ title: 'Theme Reset', description: 'Theme has been reset to defaults', variant: 'success' });
    },
    onError: () => toast({ title: 'Error', description: 'Failed to reset theme', variant: 'destructive' }),
  });

  const handleChange = (key: keyof Theme, value: string | boolean) => {
    if (localTheme) {
      setLocalTheme({ ...localTheme, [key]: value });
    }
  };

  const handleSave = () => {
    if (localTheme) {
      updateMutation.mutate(localTheme);
    }
  };

  const handleReset = () => {
    if (confirm('Reset theme to defaults? This cannot be undone.')) {
      resetMutation.mutate();
    }
  };

  const hasChanges = theme && localTheme && JSON.stringify(theme) !== JSON.stringify(localTheme);

  if (isLoading || !localTheme) {
    return <div className="flex items-center justify-center py-12">Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Theme & Branding</h1>
          <p className="text-sm text-muted-foreground">
            Customize how your menu looks to customers
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleReset} disabled={resetMutation.isPending}>
            <RotateCcw className="mr-2 h-4 w-4" />
            Reset
          </Button>
          <Button onClick={handleSave} disabled={!hasChanges || updateMutation.isPending}>
            <Save className="mr-2 h-4 w-4" />
            {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Palette className="h-5 w-5" />
                Colors
              </CardTitle>
              <CardDescription>Main colors for your brand</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="primaryColor">Primary Color</Label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    id="primaryColor"
                    value={localTheme.primaryColor}
                    onChange={(e) => handleChange('primaryColor', e.target.value)}
                    className="h-10 w-14 cursor-pointer rounded border p-1"
                  />
                  <Input
                    value={localTheme.primaryColor}
                    onChange={(e) => handleChange('primaryColor', e.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="secondaryColor">Secondary Color</Label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    id="secondaryColor"
                    value={localTheme.secondaryColor}
                    onChange={(e) => handleChange('secondaryColor', e.target.value)}
                    className="h-10 w-14 cursor-pointer rounded border p-1"
                  />
                  <Input
                    value={localTheme.secondaryColor}
                    onChange={(e) => handleChange('secondaryColor', e.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="accentColor">Accent Color</Label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    id="accentColor"
                    value={localTheme.accentColor}
                    onChange={(e) => handleChange('accentColor', e.target.value)}
                    className="h-10 w-14 cursor-pointer rounded border p-1"
                  />
                  <Input
                    value={localTheme.accentColor}
                    onChange={(e) => handleChange('accentColor', e.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="textColor">Text Color</Label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    id="textColor"
                    value={localTheme.textColor}
                    onChange={(e) => handleChange('textColor', e.target.value)}
                    className="h-10 w-14 cursor-pointer rounded border p-1"
                  />
                  <Input
                    value={localTheme.textColor}
                    onChange={(e) => handleChange('textColor', e.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="backgroundColor">Background</Label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    id="backgroundColor"
                    value={localTheme.backgroundColor}
                    onChange={(e) => handleChange('backgroundColor', e.target.value)}
                    className="h-10 w-14 cursor-pointer rounded border p-1"
                  />
                  <Input
                    value={localTheme.backgroundColor}
                    onChange={(e) => handleChange('backgroundColor', e.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="cardBackground">Card Background</Label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    id="cardBackground"
                    value={localTheme.cardBackground}
                    onChange={(e) => handleChange('cardBackground', e.target.value)}
                    className="h-10 w-14 cursor-pointer rounded border p-1"
                  />
                  <Input
                    value={localTheme.cardBackground}
                    onChange={(e) => handleChange('cardBackground', e.target.value)}
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Header Gradient</CardTitle>
              <CardDescription>Customize the header appearance</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between">
                <Label htmlFor="showGradient">Show Gradient Header</Label>
                <Switch
                  id="showGradient"
                  checked={localTheme.showGradientHeader}
                  onCheckedChange={(checked) => handleChange('showGradientHeader', checked)}
                />
              </div>
              {localTheme.showGradientHeader && (
                <>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-2">
                      <Label htmlFor="gradientStart">Gradient Start</Label>
                      <div className="flex gap-2">
                        <input
                          type="color"
                          id="gradientStart"
                          value={localTheme.gradientStart}
                          onChange={(e) => handleChange('gradientStart', e.target.value)}
                          className="h-10 w-14 cursor-pointer rounded border p-1"
                        />
                        <Input
                          value={localTheme.gradientStart}
                          onChange={(e) => handleChange('gradientStart', e.target.value)}
                        />
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="gradientEnd">Gradient End</Label>
                      <div className="flex gap-2">
                        <input
                          type="color"
                          id="gradientEnd"
                          value={localTheme.gradientEnd}
                          onChange={(e) => handleChange('gradientEnd', e.target.value)}
                          className="h-10 w-14 cursor-pointer rounded border p-1"
                        />
                        <Input
                          value={localTheme.gradientEnd}
                          onChange={(e) => handleChange('gradientEnd', e.target.value)}
                        />
                      </div>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label>Gradient Direction</Label>
                    <Select
                      value={localTheme.gradientDirection}
                      onValueChange={(v) => handleChange('gradientDirection', v)}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {gradientDirections.map((dir) => (
                          <SelectItem key={dir.value} value={dir.value}>
                            {dir.label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Typography & Style</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>Font Family</Label>
                <Select
                  value={localTheme.fontFamily}
                  onValueChange={(v) => handleChange('fontFamily', v)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {fontOptions.map((font) => (
                      <SelectItem key={font} value={font}>
                        {font}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Border Radius</Label>
                <Select
                  value={localTheme.borderRadius}
                  onValueChange={(v) => handleChange('borderRadius', v)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {radiusOptions.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Branding</CardTitle>
              <CardDescription>Logo and banner images</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="logoUrl">Logo URL</Label>
                <Input
                  id="logoUrl"
                  value={localTheme.logoUrl || ''}
                  onChange={(e) => handleChange('logoUrl', e.target.value)}
                  placeholder="https://example.com/logo.png"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="bannerUrl">Banner URL</Label>
                <Input
                  id="bannerUrl"
                  value={localTheme.bannerUrl || ''}
                  onChange={(e) => handleChange('bannerUrl', e.target.value)}
                  placeholder="https://example.com/banner.jpg"
                />
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="lg:sticky lg:top-4">
          <Card>
            <CardHeader>
              <CardTitle>Preview</CardTitle>
              <CardDescription>How your menu will look</CardDescription>
            </CardHeader>
            <CardContent>
              <div
                className="overflow-hidden rounded-lg border"
                style={{ fontFamily: localTheme.fontFamily }}
              >
                <div
                  className="p-4"
                  style={{
                    background: localTheme.showGradientHeader
                      ? `linear-gradient(${localTheme.gradientDirection === 'to-br' ? '135deg' : '90deg'}, ${localTheme.gradientStart}, ${localTheme.gradientEnd})`
                      : localTheme.primaryColor,
                  }}
                >
                  <div className="flex items-center gap-3">
                    {localTheme.logoUrl && (
                      <img
                        src={localTheme.logoUrl}
                        alt="Logo"
                        className="h-10 w-10 rounded-full object-cover"
                        onError={(e) => (e.currentTarget.style.display = 'none')}
                      />
                    )}
                    <div>
                      <h3 className="font-bold text-white">Your Restaurant</h3>
                      <p className="text-sm text-white/80">Table 1</p>
                    </div>
                  </div>
                </div>
                <div
                  className="space-y-3 p-4"
                  style={{ backgroundColor: localTheme.backgroundColor }}
                >
                  <div
                    className="overflow-hidden shadow-sm"
                    style={{
                      backgroundColor: localTheme.cardBackground,
                      borderRadius: localTheme.borderRadius === 'full' ? '1rem' : 
                        localTheme.borderRadius === '2xl' ? '1rem' :
                        localTheme.borderRadius === 'xl' ? '0.75rem' :
                        localTheme.borderRadius === 'lg' ? '0.5rem' : '0.25rem',
                    }}
                  >
                    <div className="flex">
                      <div className="h-20 w-20 bg-gray-200" />
                      <div className="flex-1 p-3">
                        <h4
                          className="font-semibold"
                          style={{ color: localTheme.textColor }}
                        >
                          Grilled Salmon
                        </h4>
                        <p
                          className="mt-1 text-xs"
                          style={{ color: `${localTheme.textColor}80` }}
                        >
                          Fresh Atlantic salmon with herbs
                        </p>
                        <p
                          className="mt-2 text-sm font-bold"
                          style={{ color: localTheme.primaryColor }}
                        >
                          $24.99
                        </p>
                      </div>
                    </div>
                  </div>
                  <button
                    className="w-full rounded-xl py-2.5 text-sm font-semibold text-white"
                    style={{
                      background: `linear-gradient(135deg, ${localTheme.gradientStart}, ${localTheme.gradientEnd})`,
                    }}
                  >
                    View Order (2 items) - $49.98
                  </button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
