import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Calendar } from 'lucide-react';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { cn } from '@/lib/utils';
import { format, subHours, subDays, subMonths, subYears } from 'date-fns';

export type TimeRange = '1h' | '6h' | '24h' | '7d' | '30d' | '90d' | '1y' | 'custom';

interface TimeRangeSelectorProps {
  value: TimeRange;
  onChange: (range: TimeRange, startDate?: Date, endDate?: Date) => void;
  showCustom?: boolean;
  className?: string;
}

const presets: { label: string; value: TimeRange }[] = [
  { label: '1 Hour', value: '1h' },
  { label: '6 Hours', value: '6h' },
  { label: '24 Hours', value: '24h' },
  { label: '7 Days', value: '7d' },
  { label: '30 Days', value: '30d' },
  { label: '90 Days', value: '90d' },
  { label: '1 Year', value: '1y' },
];

export function getDateRangeFromPreset(range: TimeRange): { start: Date; end: Date } {
  const end = new Date();
  let start: Date;

  switch (range) {
    case '1h':
      start = subHours(end, 1);
      break;
    case '6h':
      start = subHours(end, 6);
      break;
    case '24h':
      start = subDays(end, 1);
      break;
    case '7d':
      start = subDays(end, 7);
      break;
    case '30d':
      start = subDays(end, 30);
      break;
    case '90d':
      start = subDays(end, 90);
      break;
    case '1y':
      start = subYears(end, 1);
      break;
    default:
      start = subDays(end, 7);
  }

  return { start, end };
}

export function TimeRangeSelector({
  value,
  onChange,
  showCustom = true,
  className,
}: TimeRangeSelectorProps) {
  const [customStart, setCustomStart] = useState<string>('');
  const [customEnd, setCustomEnd] = useState<string>('');

  const handlePresetClick = (preset: TimeRange) => {
    onChange(preset);
  };

  const handleCustomApply = () => {
    if (customStart && customEnd) {
      onChange('custom', new Date(customStart), new Date(customEnd));
    }
  };

  return (
    <div className={cn('flex items-center gap-2', className)}>
      <div className="flex rounded-md border">
        {presets.map((preset) => (
          <Button
            key={preset.value}
            variant={value === preset.value ? 'default' : 'ghost'}
            size="sm"
            className={cn(
              'rounded-none border-r last:border-r-0',
              value === preset.value && 'bg-primary text-primary-foreground'
            )}
            onClick={() => handlePresetClick(preset.value)}
          >
            {preset.label}
          </Button>
        ))}
      </div>

      {showCustom && (
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant={value === 'custom' ? 'default' : 'outline'}
              size="sm"
              className="gap-2"
            >
              <Calendar className="h-4 w-4" />
              Custom
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-80" align="end">
            <div className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Start Date</label>
                <input
                  type="datetime-local"
                  value={customStart}
                  onChange={(e) => setCustomStart(e.target.value)}
                  className="w-full rounded-md border px-3 py-2 text-sm"
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">End Date</label>
                <input
                  type="datetime-local"
                  value={customEnd}
                  onChange={(e) => setCustomEnd(e.target.value)}
                  className="w-full rounded-md border px-3 py-2 text-sm"
                />
              </div>
              <Button onClick={handleCustomApply} className="w-full">
                Apply
              </Button>
            </div>
          </PopoverContent>
        </Popover>
      )}
    </div>
  );
}

// Compact version for smaller spaces
interface CompactTimeRangeSelectorProps {
  value: TimeRange;
  onChange: (range: TimeRange) => void;
  className?: string;
}

export function CompactTimeRangeSelector({
  value,
  onChange,
  className,
}: CompactTimeRangeSelectorProps) {
  const compactPresets = [
    { label: '1H', value: '1h' as TimeRange },
    { label: '24H', value: '24h' as TimeRange },
    { label: '7D', value: '7d' as TimeRange },
    { label: '30D', value: '30d' as TimeRange },
  ];

  return (
    <div className={cn('flex rounded-md border', className)}>
      {compactPresets.map((preset) => (
        <Button
          key={preset.value}
          variant={value === preset.value ? 'default' : 'ghost'}
          size="sm"
          className={cn(
            'h-7 rounded-none border-r px-2 text-xs last:border-r-0',
            value === preset.value && 'bg-primary text-primary-foreground'
          )}
          onClick={() => onChange(preset.value)}
        >
          {preset.label}
        </Button>
      ))}
    </div>
  );
}

// Utility to format range for display
export function formatTimeRange(range: TimeRange, startDate?: Date, endDate?: Date): string {
  if (range === 'custom' && startDate && endDate) {
    return `${format(startDate, 'MMM d, yyyy')} - ${format(endDate, 'MMM d, yyyy')}`;
  }

  const preset = presets.find((p) => p.value === range);
  return preset?.label || 'Custom';
}
