import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeftRight,
  Bookmark,
  Check,
  Columns2,
  Download,
  LayoutGrid,
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
      className="border-b border-[#E5E3DC] bg-[#FAF9F6]"
      aria-label={`Dominant outfit colors for ${look.title}`}
    >
      <div className="flex h-3 w-full border-b border-[#E5E3DC] divide-x divide-black/10">
        {swatches.map((swatch, idx) => (
          <div
            key={`${swatch.hex}-${idx}`}
            className="flex-1 h-full"
            style={{ backgroundColor: swatch.hex }}
            title={`${swatch.label}`}
          />
        ))}
      </div>
      <div className="px-4 md:px-6 py-2 flex items-center justify-between gap-2 text-[11px] text-[#68665F]">
        <span className="font-mono uppercase tracking-wider shrink-0">
          Palette
        </span>
        <div className="flex items-center gap-2 min-w-0 overflow-hidden">
          {swatches.slice(0, 4).map((swatch, idx) => (
            <span
              key={`hex-${swatch.hex}-${idx}`}
              className="inline-flex items-center gap-1 font-mono tabular-nums truncate"
              title={swatch.label}
            >
              <span
                className="w-2.5 h-2.5 border border-black/15 shrink-0"
                style={{ backgroundColor: swatch.hex }}
                aria-hidden="true"
              />
              <span className="hidden sm:inline">{swatch.hex}</span>
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

export function LookTriptych({
  looks,
  garmentTitle,
  savingLookId,
  onRetryFlatLay,
  onOpenRefine,
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
      <div className="relative aspect-[3/4] w-full bg-[#F4F3EE] border-b border-[#E5E3DC] overflow-hidden">
        {look.isGenerating ? (
          <div className="w-full h-full flex flex-col items-center justify-center p-6 md:p-8 text-center">
            <div className="w-8 h-8 border border-[#141413] border-t-transparent rounded-full animate-spin mb-4" />
            <p className="font-serif text-lg text-[#141413]">
              Composing {look.occasion} Flat-Lay
            </p>
            <p className="text-xs text-[#68665F] mt-1">
              Arranging garments on gallery paper
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
          <div className="w-full h-full flex flex-col justify-between p-5 md:p-6 bg-[#FAF9F6]">
            <div className="space-y-2">
              <p className="text-xs font-mono uppercase tracking-wider text-[#8A6A42]">
                Editorial Piece Board · {look.occasion}
              </p>
              <p className="font-serif text-lg md:text-xl text-[#141413]">
                {look.title}
              </p>
            </div>

            <div className="space-y-2 my-3 border-t border-b border-[#E5E3DC] py-3">
              {look.pieces.map((piece, pIdx) => (
                <div key={pIdx} className="flex justify-between gap-2 text-xs">
                  <span className="text-[#68665F] shrink-0">{piece.slot}</span>
                  <span className="text-[#141413] font-medium text-right truncate">
                    {piece.name}
                  </span>
                </div>
              ))}
            </div>

            <div className="space-y-3">
              {look.error && (
                <p className="text-xs text-[#7F1D1D] bg-[#FEF2F2] border border-[#9A3412]/20 p-3 leading-relaxed">
                  {look.error}
                </p>
              )}
              <button
                type="button"
                onClick={() => onRetryFlatLay(look.id)}
                className="w-full py-2.5 px-4 border border-[#141413] bg-white text-xs font-medium text-[#141413] hover:bg-[#141413] hover:text-white transition-colors flex items-center justify-center gap-2 cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" strokeWidth={1.5} />
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
      <div className="pt-4 border-t border-[#E5E3DC] space-y-2">
        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={look.isGenerating}
            onClick={() => onOpenRefine(look)}
            className="flex-1 py-2.5 px-3 border border-[#141413] bg-[#141413] text-[#FAF9F6] text-xs font-medium flex items-center justify-center gap-1.5 hover:bg-[#2B2A28] disabled:opacity-50 transition-colors whitespace-nowrap cursor-pointer"
          >
            <SlidersHorizontal className="w-3.5 h-3.5" strokeWidth={1.5} />
            <span>Refine Prompt</span>
          </button>

          <button
            type="button"
            disabled={isSaving || look.isGenerating}
            onClick={() => onSaveLook(look)}
            title={
              isSaved ? 'Saved to Wardrobe (click to update)' : 'Save to Cloud Wardrobe'
            }
            className={`py-2.5 px-3 border text-xs font-medium flex items-center gap-1.5 transition-colors whitespace-nowrap cursor-pointer ${
              isSaved
                ? 'border-[#8A6A42] bg-[#FAF9F6] text-[#8A6A42]'
                : 'border-[#E5E3DC] bg-white text-[#141413] hover:border-[#141413]'
            }`}
          >
            {isSaved ? (
              <Check className="w-3.5 h-3.5" strokeWidth={1.5} />
            ) : (
              <Bookmark className="w-3.5 h-3.5" strokeWidth={1.5} />
            )}
            <span>{isSaving ? 'Saving...' : isSaved ? 'Saved' : 'Save'}</span>
          </button>
        </div>

        {look.imageDataUrl && (
          <button
            type="button"
            onClick={() => void triggerJpegDownload(look.imageDataUrl!, jpegFilename)}
            aria-label={`Download Outfit ${look.title} as JPEG`}
            className="w-full py-2.5 px-3 border border-[#E5E3DC] bg-white text-[#141413] hover:border-[#141413] hover:bg-[#FAF9F6] text-xs font-medium flex items-center justify-center gap-1.5 transition-colors whitespace-nowrap cursor-pointer"
          >
            <Download className="w-3.5 h-3.5" strokeWidth={1.5} />
            <span>Download Outfit</span>
          </button>
        )}
      </div>
    );
  };

  return (
    <section className="max-w-[1280px] mx-auto px-6 md:px-10 py-12">
      <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-8 border-b border-[#E5E3DC]">
        <div>
          <p className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
            Flat-Lay Lookbook
          </p>
          <h2 className="font-serif text-3xl md:text-4xl font-medium text-[#141413] mt-1">
            Three Curated Occasions
          </h2>
          <p className="text-sm text-[#68665F] max-w-[54ch] mt-2">
            Each composition centers your {garmentTitle.toLowerCase()}. Refine any
            look with natural language prompts, save directly to your cloud
            wardrobe, or compare two outfits side-by-side.
          </p>
        </div>

        {canCompare && (
          <div
            role="tablist"
            aria-label="Lookbook layout mode"
            className="inline-flex border border-[#E5E3DC] bg-white p-1 self-start lg:self-auto shrink-0"
          >
            <button
              type="button"
              role="tab"
              aria-selected={viewMode === 'triptych'}
              onClick={() => setViewMode('triptych')}
              className={`px-3.5 py-2 text-xs font-medium flex items-center gap-1.5 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                viewMode === 'triptych'
                  ? 'bg-[#141413] text-[#FAF9F6]'
                  : 'text-[#68665F] hover:text-[#141413]'
              }`}
            >
              <LayoutGrid className="w-3.5 h-3.5" strokeWidth={1.5} />
              <span>Triptych View</span>
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={viewMode === 'compare'}
              onClick={() => setViewMode('compare')}
              className={`px-3.5 py-2 text-xs font-medium flex items-center gap-1.5 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                viewMode === 'compare'
                  ? 'bg-[#141413] text-[#FAF9F6]'
                  : 'text-[#68665F] hover:text-[#141413]'
              }`}
            >
              <Columns2 className="w-3.5 h-3.5" strokeWidth={1.5} />
              <span>Side-by-Side Compare</span>
            </button>
          </div>
        )}
      </div>

      {viewMode === 'compare' && canCompare && leftLook && rightLook ? (
        <div className="pt-8 space-y-6">
          {/* Comparison Pair Control Bar */}
          <div className="border border-[#E5E3DC] bg-white p-4 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
            <div className="flex-1 flex flex-col sm:flex-row sm:items-center gap-2">
              <span className="text-[11px] font-mono uppercase tracking-wider text-[#68665F] shrink-0">
                Look A
              </span>
              <div className="flex flex-wrap gap-1.5">
                {looks.map((look, idx) => {
                  const isActive = look.id === leftLook.id;
                  return (
                    <button
                      key={look.id}
                      type="button"
                      onClick={() => handleSelectLeft(look.id)}
                      className={`px-3 py-1.5 text-xs border transition-colors whitespace-nowrap cursor-pointer ${
                        isActive
                          ? 'border-[#141413] bg-[#141413] text-[#FAF9F6] font-medium'
                          : 'border-[#E5E3DC] bg-[#FAF9F6] text-[#141413] hover:border-[#141413]'
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
              className="self-center px-3 py-2 border border-[#E5E3DC] bg-[#FAF9F6] text-xs font-medium text-[#141413] hover:border-[#141413] transition-colors flex items-center gap-1.5 shrink-0 cursor-pointer"
            >
              <ArrowLeftRight className="w-3.5 h-3.5" strokeWidth={1.5} />
              <span>Swap</span>
            </button>

            <div className="flex-1 flex flex-col sm:flex-row sm:items-center sm:justify-end gap-2">
              <span className="text-[11px] font-mono uppercase tracking-wider text-[#68665F] shrink-0">
                Look B
              </span>
              <div className="flex flex-wrap gap-1.5 sm:justify-end">
                {looks.map((look, idx) => {
                  const isActive = look.id === rightLook.id;
                  return (
                    <button
                      key={look.id}
                      type="button"
                      onClick={() => handleSelectRight(look.id)}
                      className={`px-3 py-1.5 text-xs border transition-colors whitespace-nowrap cursor-pointer ${
                        isActive
                          ? 'border-[#141413] bg-[#141413] text-[#FAF9F6] font-medium'
                          : 'border-[#E5E3DC] bg-[#FAF9F6] text-[#141413] hover:border-[#141413]'
                      }`}
                    >
                      {String(idx + 1).padStart(2, '0')} · {look.occasion}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Side-by-Side Visual & Rationale Columns */}
          <div className="grid grid-cols-2 gap-3 md:gap-8">
            {[
              { label: 'Look A', look: leftLook },
              { label: 'Look B', look: rightLook },
            ].map(({ label, look }) => {
              const lookIdx = looks.findIndex((l) => l.id === look.id);
              const indexStr = String((lookIdx >= 0 ? lookIdx : 0) + 1).padStart(
                2,
                '0'
              );
              return (
                <article
                  key={`${label}-${look.id}`}
                  className="flex flex-col border border-[#E5E3DC] bg-white"
                >
                  {renderVisualFrame(look)}

                  <div className="p-4 md:p-6 flex-1 flex flex-col justify-between gap-4">
                    <div className="space-y-3">
                      <div className="flex items-center justify-between gap-2 text-xs text-[#68665F]">
                        <span className="font-mono tabular-nums truncate">
                          {label} · {indexStr} · {look.occasion}
                        </span>
                        {look.editPromptHistory.length > 0 && (
                          <span className="font-mono text-[#8A6A42] shrink-0">
                            v{look.imageHistory.length}
                          </span>
                        )}
                      </div>

                      <div>
                        <h3 className="font-serif text-xl md:text-2xl font-medium text-[#141413] leading-snug">
                          {look.title}
                        </h3>
                        <p className="text-xs md:text-sm text-[#68665F] leading-relaxed mt-2">
                          {look.stylingRationale}
                        </p>
                      </div>
                    </div>

                    {renderActionFooter(look)}
                  </div>
                </article>
              );
            })}
          </div>

          {/* Synchronized Piece-by-Piece Comparison Matrix */}
          <div className="border border-[#E5E3DC] bg-white">
            <div className="px-6 py-4 border-b border-[#E5E3DC] bg-[#FAF9F6] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <p className="text-[11px] font-mono uppercase tracking-wider text-[#8A6A42]">
                  Garment Slot Breakdown
                </p>
                <h3 className="font-serif text-xl text-[#141413]">
                  Piece-by-Piece Silhouette Comparison
                </h3>
              </div>
              <p className="text-xs text-[#68665F] font-mono tabular-nums">
                {leftLook.occasion} ({leftLook.pieces.length} pcs) vs{' '}
                {rightLook.occasion} ({rightLook.pieces.length} pcs)
              </p>
            </div>

            <div className="divide-y divide-[#E5E3DC]">
              <div className="grid grid-cols-12 px-6 py-3 bg-[#FAF9F6]/60 text-[11px] font-mono uppercase tracking-wider text-[#68665F]">
                <div className="col-span-3 md:col-span-2">Slot</div>
                <div className="col-span-4 md:col-span-5 pr-3">
                  Look A · {leftLook.occasion}
                </div>
                <div className="col-span-5 pl-3 border-l border-[#E5E3DC]">
                  Look B · {rightLook.occasion}
                </div>
              </div>

              {alignedPieceRows.map(({ slot, leftPiece, rightPiece }) => (
                <div
                  key={slot}
                  className="grid grid-cols-12 px-6 py-3.5 items-start text-xs"
                >
                  <div className="col-span-3 md:col-span-2 font-mono text-[#68665F] pr-2">
                    {slot}
                  </div>
                  <div className="col-span-4 md:col-span-5 pr-3">
                    {leftPiece ? (
                      <>
                        <p className="font-medium text-[#141413]">
                          {leftPiece.name}
                        </p>
                        <p className="text-[11px] text-[#68665F] mt-0.5">
                          {leftPiece.color} · {leftPiece.material}
                        </p>
                      </>
                    ) : (
                      <p className="text-[#68665F]/60">—</p>
                    )}
                  </div>
                  <div className="col-span-5 pl-3 border-l border-[#E5E3DC]">
                    {rightPiece ? (
                      <>
                        <p className="font-medium text-[#141413]">
                          {rightPiece.name}
                        </p>
                        <p className="text-[11px] text-[#68665F] mt-0.5">
                          {rightPiece.color} · {rightPiece.material}
                        </p>
                      </>
                    ) : (
                      <p className="text-[#68665F]/60">—</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 pt-8">
          {looks.map((look, index) => {
            const indexStr = String(index + 1).padStart(2, '0');

            return (
              <article
                key={look.id}
                className="flex flex-col border border-[#E5E3DC] bg-white"
              >
                {renderVisualFrame(look)}

                {/* Editorial Copy & Piece List */}
                <div className="p-6 flex-1 flex flex-col justify-between gap-6">
                  <div className="space-y-4">
                    <div className="flex items-center justify-between text-xs text-[#68665F]">
                      <span className="font-mono tabular-nums">
                        {indexStr} · {look.occasion}
                      </span>
                      {look.editPromptHistory.length > 0 && (
                        <span className="font-mono text-[#8A6A42]">
                          Edited · v{look.imageHistory.length}
                        </span>
                      )}
                    </div>

                    <div>
                      <h3 className="font-serif text-2xl font-medium text-[#141413] leading-snug">
                        {look.title}
                      </h3>
                      <p className="text-sm text-[#68665F] leading-relaxed mt-2">
                        {look.stylingRationale}
                      </p>
                    </div>

                    {/* Piece Breakdown */}
                    <div className="border-t border-[#E5E3DC] pt-4 space-y-2.5">
                      <p className="text-[11px] font-mono uppercase tracking-wider text-[#68665F]">
                        Outfit Pieces
                      </p>
                      <dl className="divide-y divide-[#E5E3DC]/60">
                        {look.pieces.map((piece, pIdx) => (
                          <div
                            key={pIdx}
                            className="py-2 flex items-baseline justify-between gap-4"
                          >
                            <dt className="text-xs text-[#68665F] shrink-0">
                              {piece.slot}
                            </dt>
                            <dd className="text-xs text-[#141413] text-right">
                              <span className="font-medium">{piece.name}</span>
                              <span className="block text-[11px] text-[#68665F]">
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
            );
          })}
        </div>
      )}
    </section>
  );
}

