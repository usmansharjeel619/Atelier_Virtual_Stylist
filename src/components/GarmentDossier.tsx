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

const SWATCH_ROTATIONS = ['-rotate-1', 'rotate-1', '-rotate-1', 'rotate-1', '-rotate-1'];

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
          label: 'Fit & Proportion',
          accentBg: 'bg-[#D4FF38] text-[#111111]',
          body: insight.fitAndProportion,
        },
        {
          index: '02',
          label: 'Texture & Drape',
          accentBg: 'bg-[#2B4BFF] text-white',
          body: insight.materialAndDrape,
        },
        {
          index: '03',
          label: 'How to Wear It',
          accentBg: 'bg-[#D5A8FF] text-[#111111]',
          body: insight.howToWear,
        },
        {
          index: '04',
          label: 'Skip This',
          accentBg: 'bg-[#FF3D6E] text-white',
          body: insight.whatToAvoid,
        },
      ]
    : [];

  return (
    <section className="relative max-w-[1280px] mx-auto px-4 sm:px-6 md:px-10 py-12 border-b-[3px] border-[#111111] space-y-10">
      {/* Top Piece DNA Read with Overlapping Collage Stickers */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <div className="lg:col-span-8 relative pt-3">
          <div className="flex flex-wrap items-center justify-between gap-4 mb-3">
            <div className="flex flex-wrap items-center gap-2 text-xs font-mono uppercase tracking-wider">
              <span className="-rotate-2 bg-[#2B4BFF] text-white border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] px-2.5 py-0.5 font-bold">
                Piece DNA · {analysis.category}
              </span>
              <span className="font-bold text-[#545149]">
                {brief.season} · {brief.silhouette} · {brief.budgetTier}
              </span>
            </div>

            <button
              type="button"
              onClick={handleOpenQuickTips}
              className="zine-btn rotate-1 hover:rotate-0 inline-flex items-center gap-2 px-4 py-2 bg-[#D4FF38] text-xs font-mono font-bold uppercase tracking-wider text-[#111111] whitespace-nowrap shrink-0 cursor-pointer"
            >
              <Lightbulb className="w-4 h-4" strokeWidth={2.5} />
              <span>Quick Tips</span>
            </button>
          </div>

          <h2 className="font-display text-3xl sm:text-5xl font-extrabold text-[#111111] text-balance leading-[1.03]">
            {analysis.garmentTitle}
          </h2>

          <p className="text-base text-[#111111]/90 leading-relaxed max-w-[66ch] mt-4 font-medium">
            {analysis.styleSummary}
          </p>
        </div>

        <div className="lg:col-span-4 relative pt-3">
          <div className="absolute -top-0.5 left-5 z-10 -rotate-2 bg-[#FF6B2C] text-white border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] px-2.5 py-0.5 text-[10px] font-mono font-bold uppercase">
            Texture &amp; Cut
          </div>
          <div className="zine-card-cobalt rotate-1 hover:rotate-0 transition-transform p-5 pt-6 space-y-2">
            <p className="text-sm font-medium text-[#111111] leading-relaxed">
              {analysis.fabricNotes}
            </p>
          </div>
        </div>
      </div>

      {/* Color DNA Collage Board */}
      <div className="zine-card-lg overflow-hidden">
        <div className="px-5 sm:px-6 py-4 bg-[#111111] text-white border-b-[3px] border-[#111111] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center gap-3 text-xs font-mono uppercase tracking-wider">
            <span className="-rotate-1 bg-[#D4FF38] text-[#111111] px-2.5 py-0.5 font-bold border-2 border-white">
              Color DNA
            </span>
            <span className="text-[#EBE7DA] font-bold">
              Extracted Tones · Tap to Copy
            </span>
          </div>
          <span className="text-xs font-mono font-bold tabular-nums text-[#D4FF38]">
            {analysis.palette.length} {analysis.palette.length === 1 ? 'SWATCH' : 'SWATCHES'}
          </span>
        </div>

        {analysis.palette.length > 0 && (
          <div className="flex h-4 w-full border-b-[3px] border-[#111111]" aria-hidden="true">
            {analysis.palette.map((swatch, idx) => (
              <div
                key={`bar-${swatch.hex}-${idx}`}
                className="border-r-2 last:border-r-0 border-[#111111]"
                style={{
                  backgroundColor: swatch.hex,
                  flexGrow: getSwatchWeight(swatch.role, idx),
                }}
              />
            ))}
          </div>
        )}

        <div className="p-5 sm:p-6 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 sm:gap-5">
          {analysis.palette.map((swatch, idx) => {
            const hexUpper = swatch.hex.toUpperCase();
            const isCopied = copiedHex === hexUpper;

            return (
              <div
                key={`${swatch.hex}-${idx}`}
                className={`${SWATCH_ROTATIONS[idx % SWATCH_ROTATIONS.length]} hover:rotate-0 transition-transform border-[3px] border-[#111111] shadow-[4px_4px_0_#111111] bg-[#F6F4EC] flex flex-col overflow-hidden`}
              >
                <div
                  className="h-24 w-full border-b-[3px] border-[#111111] relative"
                  style={{ backgroundColor: swatch.hex }}
                  aria-label={`${swatch.name} (${hexUpper}), role: ${swatch.role}`}
                >
                  <span className="absolute bottom-1.5 left-1.5 bg-white border-2 border-[#111111] px-1.5 py-0.5 text-[10px] font-mono font-bold uppercase text-[#111111]">
                    {swatch.role}
                  </span>
                </div>
                <div className="p-3.5 flex-1 flex flex-col justify-between gap-2 bg-white">
                  <p className="font-display text-sm font-extrabold text-[#111111] leading-snug truncate">
                    {swatch.name}
                  </p>

                  <button
                    type="button"
                    onClick={() => handleCopyHex(swatch.hex)}
                    title={`Copy ${hexUpper}`}
                    className="pt-2 border-t-2 border-[#111111]/15 flex items-center justify-between text-xs font-mono font-bold tabular-nums text-[#545149] hover:text-[#2B4BFF] transition-colors cursor-pointer"
                  >
                    <span>{hexUpper}</span>
                    {isCopied ? (
                      <span className="inline-flex items-center gap-1 bg-[#D4FF38] text-[#111111] border border-[#111111] px-1.5 py-0.5 text-[10px]">
                        <Check className="w-3 h-3" strokeWidth={2.5} />
                        <span>COPIED</span>
                      </span>
                    ) : (
                      <Copy className="w-3.5 h-3.5" strokeWidth={2} />
                    )}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Style Cheat Sheet */}
      <div className="zine-card-coral overflow-hidden">
        <div className="px-5 sm:px-6 py-4 bg-[#D5A8FF] border-b-[3px] border-[#111111] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-xs font-mono uppercase tracking-wider">
            <span className="bg-[#111111] text-[#D4FF38] px-2.5 py-0.5 font-bold">
              Style Cheat Sheet
            </span>
            <span className="text-[#111111] font-bold">
              Cut, Drape &amp; Styling Rules
            </span>
          </div>

          {onRefreshInsight && (
            <button
              type="button"
              disabled={isLoadingInsight}
              onClick={onRefreshInsight}
              className="zine-btn inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-white text-xs font-mono font-bold uppercase text-[#111111] hover:bg-[#D4FF38] disabled:opacity-50 whitespace-nowrap shrink-0 cursor-pointer self-start sm:self-auto"
            >
              <RefreshCw
                className={`w-3.5 h-3.5 ${isLoadingInsight ? 'animate-spin' : ''}`}
                strokeWidth={2.2}
              />
              <span>
                {isLoadingInsight
                  ? 'Refining...'
                  : insight
                    ? 'Remix Insight'
                    : 'Generate Insight'}
              </span>
            </button>
          )}
        </div>

        {insightError && (
          <div
            role="alert"
            className="px-6 py-3 bg-[#FFE5EC] border-b-[3px] border-[#111111] text-xs font-bold text-[#9F1239]"
          >
            {insightError}
          </div>
        )}

        {isLoadingInsight ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 divide-y-[3px] md:divide-y-0 md:divide-x-[3px] divide-[#111111]">
            {[0, 1, 2, 3].map((slot) => (
              <div key={slot} className="p-6 space-y-3 animate-pulse">
                <div className="h-4 w-24 bg-[#EBE7DA]" />
                <div className="h-3 w-full bg-[#F6F4EC]" />
                <div className="h-3 w-5/6 bg-[#F6F4EC]" />
              </div>
            ))}
          </div>
        ) : insight ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 divide-y-[3px] md:divide-y-0 md:divide-x-[3px] divide-[#111111] bg-white">
            {insightItems.map((item) => (
              <div key={item.index} className="p-6 space-y-3">
                <span
                  className={`inline-block border-2 border-[#111111] shadow-[2px_2px_0_#111111] px-2.5 py-0.5 text-xs font-mono font-bold uppercase ${item.accentBg}`}
                >
                  {item.index} · {item.label}
                </span>
                <p className="text-sm text-[#111111] leading-relaxed font-medium">{item.body}</p>
              </div>
            ))}
          </div>
        ) : (
          <div className="px-6 py-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-[#F6F4EC]">
            <p className="text-sm font-medium text-[#545149]">
              Generate tailored proportion, fabric drape, and wearing guidance for this piece.
            </p>
            {onRefreshInsight && (
              <button
                type="button"
                onClick={onRefreshInsight}
                className="zine-btn px-4 py-2 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase whitespace-nowrap shrink-0 cursor-pointer self-start sm:self-auto"
              >
                Generate Cheat Sheet
              </button>
            )}
          </div>
        )}
      </div>

      {/* Quick Tips Overlay */}
      {isQuickTipsOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/60 backdrop-blur-[2px]"
          role="dialog"
          aria-modal="true"
          aria-label="Quick Styling Tips"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              setIsQuickTipsOpen(false);
            }
          }}
        >
          <div className="w-full max-w-xl bg-[#F6F4EC] border-[4px] border-[#111111] shadow-[10px_10px_0_#D4FF38] overflow-hidden">
            <div className="px-6 py-5 bg-[#111111] text-white border-b-[3px] border-[#111111] flex items-start justify-between gap-4">
              <div className="space-y-1.5">
                <span className="inline-block bg-[#FF3D6E] text-white border-2 border-white px-2 py-0.5 text-[11px] font-mono font-bold uppercase tracking-wider">
                  Quick Styling Hits
                </span>
                <h3 className="font-display text-2xl font-extrabold text-white leading-snug">
                  {analysis.garmentTitle}
                </h3>
              </div>

              <button
                type="button"
                onClick={() => setIsQuickTipsOpen(false)}
                aria-label="Close Quick Tips"
                className="p-2 border-2 border-white text-white hover:bg-[#FF3D6E] transition-colors cursor-pointer shrink-0"
              >
                <X className="w-4 h-4" strokeWidth={2.5} />
              </button>
            </div>

            <div className="p-6 space-y-5 max-h-[70vh] overflow-y-auto">
              <p className="text-xs text-[#545149] leading-relaxed border-b-[3px] border-[#111111]/15 pb-4">
                <span className="font-mono font-bold uppercase text-[#111111]">Texture &amp; Cut: </span>
                {analysis.fabricNotes}
              </p>

              {quickTipsError && (
                <div
                  role="alert"
                  className="p-3 bg-[#FFE5EC] border-[3px] border-[#111111] text-xs font-bold text-[#9F1239]"
                >
                  {quickTipsError}
                </div>
              )}

              {isLoadingQuickTips ? (
                <div className="space-y-3 py-2">
                  {[0, 1, 2, 3].map((slot) => (
                    <div
                      key={slot}
                      className="p-4 border-[3px] border-[#111111] bg-white animate-pulse flex items-center gap-3"
                    >
                      <div className="w-6 h-3 bg-[#EBE7DA]" />
                      <div className="h-3 flex-1 bg-[#F6F4EC]" />
                    </div>
                  ))}
                </div>
              ) : quickTips.length > 0 ? (
                <ol className="divide-y-[3px] divide-[#111111] border-[3px] border-[#111111] shadow-[4px_4px_0_#111111] bg-white">
                  {quickTips.map((tip, idx) => (
                    <li key={idx} className="px-5 py-4 flex items-baseline gap-4">
                      <span className="bg-[#D4FF38] border-2 border-[#111111] px-2 py-0.5 text-xs font-mono font-bold tabular-nums text-[#111111] shrink-0">
                        {String(idx + 1).padStart(2, '0')}
                      </span>
                      <span className="text-sm font-medium text-[#111111] leading-relaxed">
                        {tip}
                      </span>
                    </li>
                  ))}
                </ol>
              ) : (
                <div className="p-6 border-[3px] border-[#111111] bg-white text-center space-y-3">
                  <p className="text-sm text-[#545149]">
                    Generate concise styling cues tailored to this piece&apos;s cut and texture.
                  </p>
                  {onRefreshQuickTips && (
                    <button
                      type="button"
                      onClick={onRefreshQuickTips}
                      className="zine-btn px-4 py-2 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase cursor-pointer"
                    >
                      Generate Quick Tips
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="px-6 py-4 bg-white border-t-[3px] border-[#111111] flex items-center justify-between gap-3">
              {onRefreshQuickTips ? (
                <button
                  type="button"
                  disabled={isLoadingQuickTips}
                  onClick={onRefreshQuickTips}
                  className="zine-btn inline-flex items-center gap-1.5 px-3.5 py-2 bg-[#F6F4EC] text-xs font-mono font-bold uppercase text-[#111111] hover:bg-[#EBE7DA] disabled:opacity-50 cursor-pointer"
                >
                  <RefreshCw
                    className={`w-3.5 h-3.5 ${isLoadingQuickTips ? 'animate-spin' : ''}`}
                    strokeWidth={2.2}
                  />
                  <span>{isLoadingQuickTips ? 'Refreshing...' : 'Remix Tips'}</span>
                </button>
              ) : (
                <span />
              )}

              <button
                type="button"
                onClick={() => setIsQuickTipsOpen(false)}
                className="zine-btn px-5 py-2 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase cursor-pointer"
              >
                Got It
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
