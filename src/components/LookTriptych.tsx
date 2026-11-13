import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeftRight,
  Bookmark,
  Check,
  Columns2,
  Download,
  LayoutGrid,
  Pencil,
  RefreshCw,
  SlidersHorizontal,
} from 'lucide-react';
import { GeneratedLookState, OutfitPiece } from '../types/stylist';
import {
  extractDominantColorsFromDataUrl,
  ExtractedSwatch,
  extractSwatchesFromPieces,
  triggerJpegDownload,
} from '../lib/imageUtils';

interface LookTriptychProps {
  looks: GeneratedLookState[];
  garmentTitle: string;
  savingLookId: string | null;
  onRetryFlatLay: (lookId: string) => void;
  onOpenRefine: (look: GeneratedLookState) => void;
  onOpenInEditor: (look: GeneratedLookState) => void;
  onSaveLook: (look: GeneratedLookState) => void;
}

type LookViewMode = 'triptych' | 'compare';

function OutfitPaletteBar({ look }: { look: GeneratedLookState }) {
  const pieceSwatches = useMemo(
    () => extractSwatchesFromPieces(look.pieces),
    [look.pieces]
  );
  const [imageSwatches, setImageSwatches] = useState<ExtractedSwatch[]>([]);

  useEffect(() => {
    let active = true;
    if (!look.imageDataUrl) {
      setImageSwatches([]);
      return;
    }

    extractDominantColorsFromDataUrl(look.imageDataUrl, 5).then((extracted) => {
      if (active) {
        setImageSwatches(extracted);
      }
    });

    return () => {
      active = false;
    };
  }, [look.imageDataUrl]);

  const swatches = imageSwatches.length > 0 ? imageSwatches : pieceSwatches;
  if (swatches.length === 0) return null;

  return (
    <div
      className="border-b-[3px] border-[#111111] bg-[#F6F4EC]"
      aria-label={`Dominant outfit colors for ${look.title}`}
    >
      <div className="flex h-4 w-full border-b-[3px] border-[#111111] divide-x-2 divide-[#111111]">
        {swatches.map((swatch, idx) => (
          <div
            key={`${swatch.hex}-${idx}`}
            className="flex-1 h-full"
            style={{ backgroundColor: swatch.hex }}
            title={`${swatch.label}`}
          />
        ))}
      </div>
      <div className="px-4 md:px-5 py-2 flex items-center justify-between gap-2 text-[11px] font-mono text-[#545149]">
        <span className="font-bold uppercase tracking-wider text-[#111111] shrink-0">
          Palette
        </span>
        <div className="flex items-center gap-2 min-w-0 overflow-hidden">
          {swatches.slice(0, 4).map((swatch, idx) => (
            <span
              key={`hex-${swatch.hex}-${idx}`}
              className="inline-flex items-center gap-1 tabular-nums truncate"
              title={swatch.label}
            >
              <span
                className="w-3 h-3 border-2 border-[#111111] shrink-0"
                style={{ backgroundColor: swatch.hex }}
                aria-hidden="true"
              />
              <span className="hidden sm:inline font-bold text-[#111111]">{swatch.hex}</span>
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

const DROP_ACCENTS = [
  {
    cardClass: 'zine-card-lime',
    badgeClass: 'bg-[#D4FF38] text-[#111111]',
    tiltClass: '-rotate-2',
  },
  {
    cardClass: 'zine-card-cobalt',
    badgeClass: 'bg-[#2B4BFF] text-white',
    tiltClass: 'rotate-1',
  },
  {
    cardClass: 'zine-card-coral',
    badgeClass: 'bg-[#FF3D6E] text-white',
    tiltClass: '-rotate-1',
  },
];

export function LookTriptych({
  looks,
  garmentTitle,
  savingLookId,
  onRetryFlatLay,
  onOpenRefine,
  onOpenInEditor,
  onSaveLook,
}: LookTriptychProps) {
  const [viewMode, setViewMode] = useState<LookViewMode>('triptych');
  const [leftLookId, setLeftLookId] = useState<string>('');
  const [rightLookId, setRightLookId] = useState<string>('');

  useEffect(() => {
    if (looks.length === 0) {
      setLeftLookId('');
      setRightLookId('');
      setViewMode('triptych');
      return;
    }

    setLeftLookId((prev) =>
      looks.some((l) => l.id === prev) ? prev : looks[0].id
    );
    setRightLookId((prev) => {
      if (looks.length < 2) return looks[0].id;
      if (looks.some((l) => l.id === prev)) return prev;
      return looks[1].id;
    });
  }, [looks]);

  const leftLook = useMemo(
    () => looks.find((l) => l.id === leftLookId) ?? looks[0],
    [looks, leftLookId]
  );

  const rightLook = useMemo(
    () =>
      looks.find((l) => l.id === rightLookId) ??
      looks[1] ??
      looks[0],
    [looks, rightLookId]
  );

  const alignedPieceRows = useMemo(() => {
    if (!leftLook || !rightLook) return [];

    const orderedSlots: string[] = [];
    const seen = new Set<string>();
    for (const p of [...leftLook.pieces, ...rightLook.pieces]) {
      const key = p.slot.trim();
      if (key && !seen.has(key.toLowerCase())) {
        seen.add(key.toLowerCase());
        orderedSlots.push(key);
      }
    }

    const findPiece = (pieces: OutfitPiece[], slot: string) =>
      pieces.find((p) => p.slot.trim().toLowerCase() === slot.toLowerCase());

    return orderedSlots.map((slot) => ({
      slot,
      leftPiece: findPiece(leftLook.pieces, slot),
      rightPiece: findPiece(rightLook.pieces, slot),
    }));
  }, [leftLook, rightLook]);

  if (looks.length === 0) return null;

  const canCompare = looks.length >= 2;

  const handleSelectLeft = (nextId: string) => {
    if (nextId === rightLook?.id && leftLook) {
      setRightLookId(leftLook.id);
    }
    setLeftLookId(nextId);
  };

  const handleSelectRight = (nextId: string) => {
    if (nextId === leftLook?.id && rightLook) {
      setLeftLookId(rightLook.id);
    }
    setRightLookId(nextId);
  };

  const handleSwapPair = () => {
    if (!leftLook || !rightLook) return;
    setLeftLookId(rightLook.id);
    setRightLookId(leftLook.id);
  };

  const renderVisualFrame = (look: GeneratedLookState) => (
    <div>
      <div className="relative aspect-[3/4] w-full bg-[#EBE7DA] border-b-[3px] border-[#111111] overflow-hidden">
        {look.isGenerating ? (
          <div className="w-full h-full flex flex-col items-center justify-center p-6 md:p-8 text-center">
            <div className="w-10 h-10 border-[3px] border-[#111111] border-t-[#D4FF38] rounded-full animate-spin mb-4" />
            <p className="font-display text-lg font-extrabold text-[#111111]">
              Styling {look.occasion} Flat-Lay
            </p>
            <p className="text-xs font-mono font-bold text-[#545149] mt-1">
              Composing fit drop...
            </p>
          </div>
        ) : look.imageDataUrl ? (
          <img
            src={look.imageDataUrl}
            alt={`${look.occasion} flat-lay outfit: ${look.title}`}
            className="w-full h-full object-cover"
            referrerPolicy="no-referrer"
          />
        ) : (
          <div className="w-full h-full flex flex-col justify-between p-5 md:p-6 bg-[#F6F4EC]">
            <div className="space-y-2 pt-2">
              <p className="text-xs font-mono font-bold uppercase tracking-wider text-[#2B4BFF]">
                Fit Recipe · {look.occasion}
              </p>
              <p className="font-display text-lg md:text-xl font-extrabold text-[#111111]">
                {look.title}
              </p>
            </div>

            <div className="space-y-2 my-3 border-t-[3px] border-b-[3px] border-[#111111] py-3">
              {look.pieces.map((piece, pIdx) => (
                <div key={pIdx} className="flex justify-between gap-2 text-xs">
                  <span className="font-mono font-bold text-[#545149] shrink-0">{piece.slot}</span>
                  <span className="text-[#111111] font-bold text-right truncate">
                    {piece.name}
                  </span>
                </div>
              ))}
            </div>

            <div className="space-y-3">
              {look.error && (
                <p className="text-xs font-bold text-[#9F1239] bg-[#FFE5EC] border-[3px] border-[#111111] p-3 leading-relaxed">
                  {look.error}
                </p>
              )}
              <button
                type="button"
                onClick={() => onRetryFlatLay(look.id)}
                className="zine-btn w-full py-2.5 px-4 bg-[#D4FF38] text-xs font-mono font-bold uppercase text-[#111111] flex items-center justify-center gap-2 cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" strokeWidth={2.5} />
                <span>Generate Flat-Lay Photo</span>
              </button>
            </div>
          </div>
        )}
      </div>
      <OutfitPaletteBar look={look} />
    </div>
  );

  const renderActionFooter = (look: GeneratedLookState) => {
    const isSaving = savingLookId === look.id;
    const isSaved = Boolean(look.savedLookId);
    const jpegFilename = `atelier-${look.occasion.toLowerCase().replace(/\s+/g, '-')}-outfit.jpg`;

    return (
      <div className="pt-4 border-t-[3px] border-[#111111]/20 space-y-2.5">
        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={look.isGenerating}
            onClick={() => onOpenInEditor(look)}
            className="zine-btn flex-1 py-2.5 px-3 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase flex items-center justify-center gap-1.5 disabled:opacity-50 whitespace-nowrap cursor-pointer"
          >
            <Pencil className="w-3.5 h-3.5" strokeWidth={2.5} />
            <span>Open in Editor</span>
          </button>

          <button
            type="button"
            disabled={look.isGenerating}
            onClick={() => onOpenRefine(look)}
            className="zine-btn py-2.5 px-3 bg-white text-[#111111] hover:bg-[#D5A8FF] text-xs font-mono font-bold uppercase flex items-center justify-center gap-1.5 disabled:opacity-50 whitespace-nowrap cursor-pointer"
          >
            <SlidersHorizontal className="w-3.5 h-3.5" strokeWidth={2.2} />
            <span>Tweak</span>
          </button>

          <button
            type="button"
            disabled={isSaving || look.isGenerating}
            onClick={() => onSaveLook(look)}
            title={
              isSaved ? 'Saved to Closet (click to update)' : 'Save to Cloud Closet'
            }
            aria-label={isSaved ? 'Saved to Closet' : 'Save to Cloud Closet'}
            className={`zine-btn py-2.5 px-3 text-xs font-mono font-bold uppercase flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
              isSaved
                ? 'bg-[#FF3D6E] text-white'
                : 'bg-white text-[#111111] hover:bg-[#EBE7DA]'
            }`}
          >
            {isSaved ? (
              <Check className="w-4 h-4" strokeWidth={2.5} />
            ) : (
              <Bookmark className="w-4 h-4" strokeWidth={2.2} />
            )}
            <span className="hidden xl:inline">{isSaving ? 'Saving' : isSaved ? 'Saved' : 'Save'}</span>
          </button>
        </div>

        {look.imageDataUrl && (
          <button
            type="button"
            onClick={() => void triggerJpegDownload(look.imageDataUrl!, jpegFilename)}
            aria-label={`Download Outfit ${look.title} as JPEG`}
            className="w-full py-2.5 px-3 border-[3px] border-[#111111] bg-[#F6F4EC] text-[#111111] hover:bg-[#D4FF38] text-xs font-mono font-bold uppercase flex items-center justify-center gap-1.5 transition-colors whitespace-nowrap cursor-pointer"
          >
            <Download className="w-3.5 h-3.5" strokeWidth={2.2} />
            <span>Download Outfit</span>
          </button>
        )}
      </div>
    );
  };

  return (
    <section className="max-w-[1280px] mx-auto px-4 sm:px-6 md:px-10 py-12">
      <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-8 border-b-[3px] border-[#111111]">
        <div className="space-y-2">
          <span className="inline-block -rotate-2 bg-[#FF6B2C] text-white border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] px-3 py-0.5 text-xs font-mono font-bold uppercase tracking-wider">
            3 Occasion Drops
          </span>
          <h2 className="font-display text-3xl md:text-5xl font-extrabold text-[#111111]">
            Curated Fit Lineup
          </h2>
          <p className="text-sm font-medium text-[#545149] max-w-[54ch]">
            Every drop locks in your {garmentTitle.toLowerCase()}. Open any look in the Editor,
            tweak with natural language prompts, or face two fits off 1v1.
          </p>
        </div>

        {canCompare && (
          <div
            role="tablist"
            aria-label="Lookbook layout mode"
            className="-rotate-1 inline-flex border-[3px] border-[#111111] bg-white p-1.5 gap-1.5 self-start lg:self-auto shrink-0 shadow-[5px_5px_0_#111111]"
          >
            <button
              type="button"
              role="tab"
              aria-selected={viewMode === 'triptych'}
              onClick={() => setViewMode('triptych')}
              className={`px-3.5 py-2 text-xs font-mono font-bold uppercase flex items-center gap-1.5 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                viewMode === 'triptych'
                  ? 'bg-[#D4FF38] text-[#111111] border-2 border-[#111111]'
                  : 'text-[#545149] hover:text-[#111111]'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" strokeWidth={2.5} />
              <span>Look Drop</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={viewMode === 'compare'}
              onClick={() => setViewMode('compare')}
              className={`px-3.5 py-2 text-xs font-mono font-bold uppercase flex items-center gap-1.5 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                viewMode === 'compare'
                  ? 'bg-[#2B4BFF] text-white border-2 border-[#111111]'
                  : 'text-[#545149] hover:text-[#111111]'
              }`}
            >
              <Columns2 className="w-3.5 h-3.5" strokeWidth={2.5} />
              <span>Compare 1v1</span>
            </button>
          </div>
        )}
      </div>

      {viewMode === 'compare' && canCompare && leftLook && rightLook ? (
        <div className="pt-9 space-y-8">
          <div className="zine-card-cobalt p-4 sm:p-5 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
            <div className="flex-1 flex flex-col sm:flex-row sm:items-center gap-2.5">
              <span className="-rotate-2 inline-block bg-[#D4FF38] text-[#111111] border-2 border-[#111111] px-2 py-0.5 text-[11px] font-mono font-bold uppercase tracking-wider shrink-0">
                Side A
              </span>
              <div className="flex flex-wrap gap-1.5">
                {looks.map((look, idx) => {
                  const isActive = look.id === leftLook.id;
                  return (
                    <button
                      key={look.id}
                      type="button"
                      onClick={() => handleSelectLeft(look.id)}
                      className={`px-3 py-1.5 text-xs font-mono font-bold border-2 border-[#111111] transition-all whitespace-nowrap cursor-pointer ${
                        isActive
                          ? '-translate-y-0.5 bg-[#D4FF38] text-[#111111] shadow-[3px_3px_0_#111111]'
                          : 'bg-[#F6F4EC] text-[#111111] hover:bg-[#EBE7DA]'
                      }`}
                    >
                      {String(idx + 1).padStart(2, '0')} · {look.occasion}
                    </button>
                  );
                })}
              </div>
            </div>

            <button
              type="button"
              onClick={handleSwapPair}
              aria-label="Swap left and right outfits"
              className="zine-btn self-center px-3.5 py-2 bg-white text-xs font-mono font-bold uppercase text-[#111111] hover:bg-[#D5A8FF] flex items-center gap-1.5 shrink-0 cursor-pointer"
            >
              <ArrowLeftRight className="w-3.5 h-3.5" strokeWidth={2.5} />
              <span>Swap</span>
            </button>

            <div className="flex-1 flex flex-col sm:flex-row sm:items-center sm:justify-end gap-2.5">
              <span className="rotate-2 inline-block bg-[#FF3D6E] text-white border-2 border-[#111111] px-2 py-0.5 text-[11px] font-mono font-bold uppercase tracking-wider shrink-0">
                Side B
              </span>
              <div className="flex flex-wrap gap-1.5 sm:justify-end">
                {looks.map((look, idx) => {
                  const isActive = look.id === rightLook.id;
                  return (
                    <button
                      key={look.id}
                      type="button"
                      onClick={() => handleSelectRight(look.id)}
                      className={`px-3 py-1.5 text-xs font-mono font-bold border-2 border-[#111111] transition-all whitespace-nowrap cursor-pointer ${
                        isActive
                          ? '-translate-y-0.5 bg-[#2B4BFF] text-white shadow-[3px_3px_0_#111111]'
                          : 'bg-[#F6F4EC] text-[#111111] hover:bg-[#EBE7DA]'
                      }`}
                    >
                      {String(idx + 1).padStart(2, '0')} · {look.occasion}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="relative grid grid-cols-1 md:grid-cols-2 gap-8 md:gap-10 items-stretch">
            {/* Overlapping Center VS Stamp on desktop */}
            <div
              aria-hidden="true"
              className="hidden md:flex absolute left-1/2 top-1/3 -translate-x-1/2 -translate-y-1/2 z-20 rotate-6 bg-[#D4FF38] text-[#111111] border-[4px] border-[#111111] shadow-[5px_5px_0_#FF3D6E] px-4 py-2 font-display text-2xl font-extrabold uppercase tracking-tight"
            >
              VS
            </div>

            {[
              {
                label: 'Side A',
                look: leftLook,
                cardClass: 'zine-card-lime',
                badgeBg: 'bg-[#D4FF38] text-[#111111]',
                tilt: '-rotate-2',
              },
              {
                label: 'Side B',
                look: rightLook,
                cardClass: 'zine-card-coral',
                badgeBg: 'bg-[#FF3D6E] text-white',
                tilt: 'rotate-2',
              },
            ].map(({ label, look, cardClass, badgeBg, tilt }) => {
              const lookIdx = looks.findIndex((l) => l.id === look.id);
              const indexStr = String((lookIdx >= 0 ? lookIdx : 0) + 1).padStart(2, '0');
              return (
                <div key={`${label}-${look.id}`} className="relative pt-3 flex">
                  <div
                    className={`absolute -top-0.5 left-4 z-10 ${tilt} ${badgeBg} zine-sticker px-3 py-1 text-xs font-mono font-bold uppercase`}
                  >
                    {label} · {indexStr} · {look.occasion}
                  </div>

                  <article className={`${cardClass} flex-1 flex flex-col overflow-hidden`}>
                    {renderVisualFrame(look)}

                    <div className="p-5 md:p-6 flex-1 flex flex-col justify-between gap-4">
                      <div className="space-y-3">
                        <div className="flex items-center justify-between gap-2 text-xs font-mono">
                          <span className="font-bold uppercase text-[#2B4BFF]">
                            Head-to-Head Matchup
                          </span>
                          {look.editPromptHistory.length > 0 && (
                            <span className="bg-[#FF3D6E] text-white border-2 border-[#111111] px-2 py-0.5 font-bold shrink-0">
                              v{look.imageHistory.length}
                            </span>
                          )}
                        </div>

                        <div>
                          <h3 className="font-display text-xl md:text-2xl font-extrabold text-[#111111] leading-snug">
                            {look.title}
                          </h3>
                          <p className="text-xs md:text-sm font-medium text-[#545149] leading-relaxed mt-2">
                            {look.stylingRationale}
                          </p>
                        </div>
                      </div>

                      {renderActionFooter(look)}
                    </div>
                  </article>
                </div>
              );
            })}
          </div>

          <div className="zine-card-lg overflow-hidden">
            <div className="px-6 py-4 border-b-[3px] border-[#111111] bg-[#111111] text-white flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="space-y-1">
                <span className="inline-block bg-[#D4FF38] text-[#111111] px-2 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider">
                  Slot-by-Slot Breakdown
                </span>
                <h3 className="font-display text-xl font-extrabold text-white">
                  Piece-by-Piece Fit Matchup
                </h3>
              </div>
              <p className="text-xs text-[#D4FF38] font-mono font-bold tabular-nums">
                {leftLook.occasion} ({leftLook.pieces.length} pcs) vs {rightLook.occasion} (
                {rightLook.pieces.length} pcs)
              </p>
            </div>

            <div className="divide-y-2 divide-[#111111]">
              <div className="grid grid-cols-12 px-6 py-3 bg-[#D5A8FF] text-[11px] font-mono font-bold uppercase tracking-wider text-[#111111]">
                <div className="col-span-3 md:col-span-2">Slot</div>
                <div className="col-span-4 md:col-span-5 pr-3">Side A · {leftLook.occasion}</div>
                <div className="col-span-5 pl-3 border-l-2 border-[#111111]">
                  Side B · {rightLook.occasion}
                </div>
              </div>

              {alignedPieceRows.map(({ slot, leftPiece, rightPiece }) => (
                <div key={slot} className="grid grid-cols-12 px-6 py-3.5 items-start text-xs bg-white">
                  <div className="col-span-3 md:col-span-2 font-mono font-bold text-[#2B4BFF] pr-2 uppercase">
                    {slot}
                  </div>
                  <div className="col-span-4 md:col-span-5 pr-3">
                    {leftPiece ? (
                      <>
                        <p className="font-bold text-[#111111]">{leftPiece.name}</p>
                        <p className="text-[11px] font-mono text-[#545149] mt-0.5">
                          {leftPiece.color} · {leftPiece.material}
                        </p>
                      </>
                    ) : (
                      <p className="text-[#545149]/60">—</p>
                    )}
                  </div>
                  <div className="col-span-5 pl-3 border-l-2 border-[#111111]">
                    {rightPiece ? (
                      <>
                        <p className="font-bold text-[#111111]">{rightPiece.name}</p>
                        <p className="text-[11px] font-mono text-[#545149] mt-0.5">
                          {rightPiece.color} · {rightPiece.material}
                        </p>
                      </>
                    ) : (
                      <p className="text-[#545149]/60">—</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 pt-9">
          {looks.map((look, index) => {
            const indexStr = String(index + 1).padStart(2, '0');
            const accent = DROP_ACCENTS[index % DROP_ACCENTS.length];

            return (
              <div key={look.id} className="relative pt-3 flex">
                {/* Overlapping Skewed Occasion Sticker */}
                <div
                  className={`absolute -top-0.5 left-4 z-10 ${accent.tiltClass} ${accent.badgeClass} zine-sticker px-3 py-1 text-xs font-mono font-bold uppercase tabular-nums`}
                >
                  {indexStr} · {look.occasion}
                </div>

                {look.editPromptHistory.length > 0 && (
                  <div className="absolute -top-0.5 right-4 z-10 rotate-2 bg-[#111111] text-[#D4FF38] border-[3px] border-[#111111] shadow-[3px_3px_0_#FF3D6E] px-2.5 py-0.5 text-[11px] font-mono font-bold uppercase">
                    REMIXED · v{look.imageHistory.length}
                  </div>
                )}

                <article className={`${accent.cardClass} flex-1 flex flex-col overflow-hidden`}>
                  {renderVisualFrame(look)}

                  <div className="p-5 sm:p-6 flex-1 flex flex-col justify-between gap-6">
                    <div className="space-y-4">
                      <div>
                        <h3 className="font-display text-2xl font-extrabold text-[#111111] leading-snug">
                          {look.title}
                        </h3>
                        <p className="text-sm font-medium text-[#545149] leading-relaxed mt-2">
                          {look.stylingRationale}
                        </p>
                      </div>

                      <div className="border-t-[3px] border-[#111111]/20 pt-4 space-y-2.5">
                        <p className="inline-block bg-[#EBE7DA] border-2 border-[#111111] px-2 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider text-[#111111]">
                          Pieces in This Fit
                        </p>
                        <dl className="divide-y-2 divide-[#111111]/15">
                          {look.pieces.map((piece, pIdx) => (
                            <div
                              key={pIdx}
                              className="py-2 flex items-baseline justify-between gap-4"
                            >
                              <dt className="text-xs font-mono font-bold text-[#2B4BFF] shrink-0">
                                {piece.slot}
                              </dt>
                              <dd className="text-xs text-[#111111] text-right">
                                <span className="font-bold">{piece.name}</span>
                                <span className="block text-[11px] font-mono text-[#545149]">
                                  {piece.color} · {piece.material}
                                </span>
                              </dd>
                            </div>
                          ))}
                        </dl>
                      </div>
                    </div>

                    {renderActionFooter(look)}
                  </div>
                </article>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
