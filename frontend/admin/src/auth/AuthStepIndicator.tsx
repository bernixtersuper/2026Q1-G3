interface AuthStepIndicatorProps {
  steps: string[];
  current: number;
}

export function AuthStepIndicator({ steps, current }: AuthStepIndicatorProps) {
  return (
    <ol className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
      {steps.map((label, index) => {
        const step = index + 1;
        const active = step === current;
        const done = step < current;
        return (
          <li key={label} className="flex items-center gap-2">
            {index > 0 && <span className="text-border" aria-hidden>›</span>}
            <span
              className={
                active
                  ? 'font-medium text-primary'
                  : done
                    ? 'text-foreground'
                    : undefined
              }
            >
              <span
                className={
                  active || done
                    ? 'mr-1 inline-flex h-5 w-5 items-center justify-center rounded-full bg-primary/10 text-[10px] font-semibold text-primary'
                    : 'mr-1 inline-flex h-5 w-5 items-center justify-center rounded-full bg-muted text-[10px] font-semibold'
                }
              >
                {step}
              </span>
              {label}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
