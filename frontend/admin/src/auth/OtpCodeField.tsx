import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';

interface OtpCodeFieldProps {
  id: string;
  label?: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  maxLength?: number;
}

export function OtpCodeField({
  id,
  label = 'Verification code',
  value,
  onChange,
  disabled,
  maxLength = 6,
}: OtpCodeFieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        inputMode="numeric"
        autoComplete="one-time-code"
        placeholder={'·'.repeat(maxLength)}
        value={value}
        onChange={(e) => onChange(e.target.value.replace(/\D/g, '').slice(0, maxLength))}
        disabled={disabled}
        maxLength={maxLength}
        className={cn(
          'h-12 text-center font-mono text-lg tracking-[0.35em]',
          'placeholder:tracking-[0.35em] placeholder:text-muted-foreground/40'
        )}
      />
      <p className="text-xs text-muted-foreground text-center">
        Enter the {maxLength}-digit code from your email
      </p>
    </div>
  );
}
