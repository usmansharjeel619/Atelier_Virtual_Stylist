import { useEffect, useState } from 'react';
import { Check, Copy, Lightbulb, RefreshCw, X } from 'lucide-react';
import { GarmentAnalysis, StyleBrief } from '../types/stylist';

interface GarmentDossierProps {
  analysis: GarmentAnalysis;
  brief: StyleBrief;
  isLoadingInsight?: boolean;
  insightError?: string | null;
  onRefreshInsight?: () => void;
  isLoadingQuickTips?: boolean;
  quickTipsError?: string | null;
  onRefreshQuickTips?: () => void;
}

function getSwatchWeight(role: string, index: number): number {
  const lower = role.toLowerCase();
  if (lower.includes('primary') || lower.includes('dominant') || index === 0) return 3;
  if (lower.includes('secondary') || lower.includes('bridge')) return 2;
  return 1;
}

export function GarmentDossier({
  analysis,
  brief,
  isLoadingInsight = false,
  insightError = null,
  onRefreshInsight,
  isLoadingQuickTips = false,
  quickTipsError = null,
  onRefreshQuickTips,
}: GarmentDossierProps) {
  const [copiedHex, setCopiedHex] = useState<string | null>(null);
  const [isQuickTipsOpen, setIsQuickTipsOpen] = useState(false);

  const insight = analysis.styleInsight;
  const quickTips = analysis.quickTips ?? [];

  useEffect(() => {
    if (!isQuickTipsOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsQuickTipsOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isQuickTipsOpen]);

  const handleOpenQuickTips = () => {
    setIsQuickTipsOpen(true);
    if (quickTips.length === 0 && !isLoadingQuickTips && onRefreshQuickTips) {
      onRefreshQuickTips();
    }
  };

  const handleCopyHex = async (hex: string) => {
    const normalized = hex.toUpperCase();
    try {
      await navigator.clipboard.writeText(normalized);
      setCopiedHex(normalized);
      window.setTimeout(() => {
        setCopiedHex((prev) => (prev === normalized ? null : prev));
      }, 1800);
    } catch {
      // Ignore clipboard permission errors in restricted contexts
    }
  };

  const insightItems = insight
    ? [
        {
          index: '01',
          label: 'Proportion & Cut',
          body: insight.fitAndProportion,
        },
        {
          index: '02',
          label: 'Material & Drape',
          body: insight.materialAndDrape,
        },
        {
          index: '03',
          label: 'How to Wear It',
          body: insight.howToWear,
        },
        {
          index: '04',
          label: 'What to Avoid',
          body: insight.whatToAvoid,
        },
      ]
    : [];

  return (
    <section className="relative max-w-[1280px] mx-auto px-6 md:px-10 py-10 border-b border-[#E5E3DC] space-y-8">
      {/* Top Editorial Garment Read */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <div className="lg:col-span-8 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex flex-wrap items-center gap-2 text-xs text-[#68665F]">
              <span className="font-mono uppercase tracking-wider text-[#8A6A42]">Garment Dossier</span>
              <span aria-hidden="true">·</span>
              <span>{analysis.category}</span>
              <span aria-hidden="true">·</span>
              <span>{brief.season}</span>
              <span aria-hidden="true">·</span>
              <span>{brief.silhouette}</span>
              <span aria-hidden="true">·</span>
              <span>{brief.budgetTier}</span>
            </div>

            <button
              type="button"
              onClick={handleOpenQuickTips}
              className="inline-flex items-center gap-2 px-4 py-2 border border-[#141413] bg-white text-xs font-medium text-[#141413] hover:bg-[#141413] hover:text-[#FAF9F6] transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8A6A42]"
            >
              <Lightbulb className="w-3.5 h-3.5" strokeWidth={1.5} />
              <span>Quick Tips</span>
            </button>
          </div>

          <h2 className="font-serif text-3xl md:text-4xl font-medium text-[#141413] text-balance">
            {analysis.garmentTitle}
          </h2>

          <p className="text-base text-[#141413]/85 leading-relaxed max-w-[68ch]">
            {analysis.styleSummary}
          </p>
        </div>

        <div className="lg:col-span-4 border border-[#E5E3DC] bg-white p-5 space-y-2.5">
          <p className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
            Material &amp; Pattern Read
          </p>
          <p className="text-sm text-[#141413]/85 leading-relaxed">{analysis.fabricNotes}</p>
        </div>
      </div>

      {/* Visual Color Palette Grid */}
      <div className="border border-[#E5E3DC] bg-white">
        <div className="px-6 py-4 border-b border-[#E5E3DC] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center gap-2 text-xs">
            <span className="font-mono uppercase tracking-wider text-[#8A6A42]">
              Color Palette
            </span>
            <span aria-hidden="true" className="text-[#68665F]">
              ·
            </span>
            <span className="text-[#68665F]">
              Extracted garment tones and their role in the look
            </span>
          </div>
          <span className="text-xs font-mono tabular-nums text-[#68665F]">
            {analysis.palette.length} {analysis.palette.length === 1 ? 'Swatch' : 'Swatches'}
          </span>
        </div>

        {/* Continuous Tonal Proportion Bar */}
        {analysis.palette.length > 0 && (
          <div className="flex h-3 w-full border-b border-[#E5E3DC]" aria-hidden="true">
            {analysis.palette.map((swatch, idx) => (
              <div
                key={`bar-${swatch.hex}-${idx}`}
                style={{
                  backgroundColor: swatch.hex,
                  flexGrow: getSwatchWeight(swatch.role, idx),
                }}
              />
            ))}
          </div>
        )}

        <div className="p-6 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {analysis.palette.map((swatch, idx) => {
            const hexUpper = swatch.hex.toUpperCase();
            const isCopied = copiedHex === hexUpper;

            return (
              <div
                key={`${swatch.hex}-${idx}`}
                className="border border-[#E5E3DC] bg-[#FAF9F6] flex flex-col overflow-hidden"
              >
                <div
                  className="h-24 w-full border-b border-black/10"
                  style={{ backgroundColor: swatch.hex }}
                  aria-label={`${swatch.name} (${hexUpper}), role: ${swatch.role}`}
                />
                <div className="p-3.5 flex-1 flex flex-col justify-between gap-2">
                  <div className="space-y-1">
                    <p className="text-[11px] font-mono uppercase tracking-wider text-[#8A6A42] truncate">
                      {swatch.role}
                    </p>
                    <p className="text-sm font-medium text-[#141413] leading-snug truncate">
                      {swatch.name}
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleCopyHex(swatch.hex)}
                    title={`Copy ${hexUpper}`}
                    className="pt-2 border-t border-[#E5E3DC]/80 flex items-center justify-between text-xs font-mono tabular-nums text-[#68665F] hover:text-[#141413] transition-colors cursor-pointer"
                  >
                    <span>{hexUpper}</span>
                    {isCopied ? (
                      <span className="inline-flex items-center gap-1 text-[#8A6A42]">
                        <Check className="w-3 h-3" strokeWidth={1.75} />
                        <span>Copied</span>
                      </span>
                    ) : (
                      <Copy className="w-3 h-3" strokeWidth={1.5} />
                    )}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Style Insight Section: Actionable Material & Cut Guidance */}
      <div className="border border-[#E5E3DC] bg-white">
        <div className="px-6 py-4 border-b border-[#E5E3DC] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-xs">
            <span className="font-mono uppercase tracking-wider text-[#8A6A42]">
              Style Insight
            </span>
            <span aria-hidden="true" className="text-[#68665F]">
              ·
            </span>
            <span className="text-[#68665F]">
              Actionable wearing guidance by material and cut
            </span>
          </div>

          {onRefreshInsight && (
            <button
              type="button"
              disabled={isLoadingInsight}
              onClick={onRefreshInsight}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-[#E5E3DC] bg-[#FAF9F6] text-xs font-medium text-[#141413] hover:border-[#141413] disabled:opacity-50 transition-colors whitespace-nowrap shrink-0 cursor-pointer self-start sm:self-auto"
            >
              <RefreshCw
                className={`w-3.5 h-3.5 ${isLoadingInsight ? 'animate-spin' : ''}`}
                strokeWidth={1.5}
              />
              <span>
                {isLoadingInsight
                  ? 'Refining Insight...'
                  : insight
                    ? 'Refresh Style Insight'
                    : 'Generate Style Insight'}
              </span>
            </button>
          )}
        </div>

        {insightError && (
          <div
            role="alert"
            className="px-6 py-3 bg-[#FEF2F2] border-b border-[#9A3412]/20 text-xs text-[#7F1D1D]"
          >
            {insightError}
          </div>
        )}

        {isLoadingInsight ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 divide-y md:divide-y-0 md:divide-x divide-[#E5E3DC]">
            {[0, 1, 2, 3].map((slot) => (
              <div key={slot} className="p-6 space-y-3 animate-pulse">
                <div className="h-3 w-24 bg-[#E5E3DC]" />
                <div className="h-3 w-full bg-[#F4F3EE]" />
                <div className="h-3 w-5/6 bg-[#F4F3EE]" />
                <div className="h-3 w-2/3 bg-[#F4F3EE]" />
              </div>
            ))}
          </div>
        ) : insight ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 divide-y md:divide-y-0 md:divide-x divide-[#E5E3DC]">
            {insightItems.map((item) => (
              <div key={item.index} className="p-6 space-y-2.5">
                <p className="text-xs font-mono text-[#68665F]">
                  <span className="text-[#8A6A42]">{item.index}.</span> {item.label}
                </p>
                <p className="text-sm text-[#141413]/90 leading-relaxed">{item.body}</p>
              </div>
            ))}
          </div>
        ) : (
          <div className="px-6 py-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-[#FAF9F6]">
            <p className="text-sm text-[#68665F]">
              Generate tailored proportion, fabric drape, and wearing guidance for this piece.
            </p>
            {onRefreshInsight && (
              <button
                type="button"
                onClick={onRefreshInsight}
                className="px-4 py-2 bg-[#141413] text-[#FAF9F6] text-xs font-medium hover:bg-[#2B2A28] transition-colors whitespace-nowrap shrink-0 cursor-pointer self-start sm:self-auto"
              >
                Generate Style Insight
              </button>
            )}
          </div>
        )}
      </div>

      {/* Quick Tips Overlay */}
      {isQuickTipsOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/45 backdrop-blur-[2px]"
          role="dialog"
          aria-modal="true"
          aria-label="Quick Styling Tips"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              setIsQuickTipsOpen(false);
            }
          }}
        >
          <div className="w-full max-w-xl bg-[#FAF9F6] border border-[#141413] shadow-xl overflow-hidden">
            {/* Overlay Header */}
            <div className="px-6 py-5 bg-white border-b border-[#E5E3DC] flex items-start justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-xs text-[#68665F]">
                  <span className="font-mono uppercase tracking-wider text-[#8A6A42]">
                    Quick Tips
                  </span>
                  <span aria-hidden="true">·</span>
                  <span>Texture &amp; Cut Cues</span>
                </div>
                <h3 className="font-serif text-2xl font-medium text-[#141413] leading-snug">
                  {analysis.garmentTitle}
                </h3>
              </div>

              <button
                type="button"
                onClick={() => setIsQuickTipsOpen(false)}
                aria-label="Close Quick Tips"
                className="p-2 border border-[#E5E3DC] text-[#141413] hover:border-[#141413] transition-colors cursor-pointer shrink-0"
              >
                <X className="w-4 h-4" strokeWidth={1.5} />
              </button>
            </div>

            {/* Overlay Body */}
            <div className="p-6 space-y-5 max-h-[70vh] overflow-y-auto">
              <p className="text-xs text-[#68665F] leading-relaxed border-b border-[#E5E3DC] pb-4">
                <span className="font-medium text-[#141413]">Cut &amp; Texture Basis: </span>
                {analysis.fabricNotes}
              </p>

              {quickTipsError && (
                <div
                  role="alert"
                  className="p-3 bg-[#FEF2F2] border border-[#9A3412]/20 text-xs text-[#7F1D1D]"
                >
                  {quickTipsError}
                </div>
              )}

              {isLoadingQuickTips ? (
                <div className="space-y-3 py-2">
                  {[0, 1, 2, 3].map((slot) => (
                    <div
                      key={slot}
                      className="p-4 border border-[#E5E3DC] bg-white animate-pulse flex items-center gap-3"
                    >
                      <div className="w-6 h-3 bg-[#E5E3DC]" />
                      <div className="h-3 flex-1 bg-[#F4F3EE]" />
                    </div>
                  ))}
                </div>
              ) : quickTips.length > 0 ? (
                <ol className="divide-y divide-[#E5E3DC] border border-[#E5E3DC] bg-white">
                  {quickTips.map((tip, idx) => (
                    <li key={idx} className="px-5 py-4 flex items-baseline gap-4">
                      <span className="text-xs font-mono tabular-nums text-[#8A6A42] shrink-0">
                        {String(idx + 1).padStart(2, '0')}
                      </span>
                      <span className="text-sm text-[#141413] leading-relaxed">{tip}</span>
                    </li>
                  ))}
                </ol>
              ) : (
                <div className="p-6 border border-[#E5E3DC] bg-white text-center space-y-3">
                  <p className="text-sm text-[#68665F]">
                    Generate concise styling cues tailored to this garment&apos;s cut and texture.
                  </p>
                  {onRefreshQuickTips && (
                    <button
                      type="button"
                      onClick={onRefreshQuickTips}
                      className="px-4 py-2 bg-[#141413] text-[#FAF9F6] text-xs font-medium hover:bg-[#2B2A28] cursor-pointer"
                    >
                      Generate Quick Tips
                    </button>
                  )}
                </div>
              )}
            </div>

            {/* Overlay Footer */}
            <div className="px-6 py-4 bg-white border-t border-[#E5E3DC] flex items-center justify-between gap-3">
              {onRefreshQuickTips ? (
                <button
                  type="button"
                  disabled={isLoadingQuickTips}
                  onClick={onRefreshQuickTips}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 border border-[#E5E3DC] bg-[#FAF9F6] text-xs font-medium text-[#141413] hover:border-[#141413] disabled:opacity-50 transition-colors cursor-pointer"
                >
                  <RefreshCw
                    className={`w-3.5 h-3.5 ${isLoadingQuickTips ? 'animate-spin' : ''}`}
                    strokeWidth={1.5}
                  />
                  <span>{isLoadingQuickTips ? 'Refreshing...' : 'Refresh Tips'}</span>
                </button>
              ) : (
                <span />
              )}

              <button
                type="button"
                onClick={() => setIsQuickTipsOpen(false)}
                className="px-5 py-2 bg-[#141413] text-[#FAF9F6] text-xs font-medium hover:bg-[#2B2A28] transition-colors cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
