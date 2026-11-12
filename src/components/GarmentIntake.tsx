import React, { useRef, useState } from 'react';
import { Upload, ArrowRight, RotateCcw } from 'lucide-react';
import {
  BudgetTierOption,
  SampleGarment,
  SeasonOption,
  SilhouetteOption,
  StyleBrief,
} from '../types/stylist';
import { HERO_FLATLAY_IMAGE, SAMPLE_GARMENTS } from '../data/sampleGarments';

interface GarmentIntakeProps {
  sourceImageDataUrl: string | null;
  activeGarmentLabel: string | null;
  brief: StyleBrief;
  isAnalyzing: boolean;
  analyzeError: string | null;
  onChangeBrief: (next: StyleBrief) => void;
  onUploadFile: (file: File) => void;
  onSelectSample: (sample: SampleGarment) => void;
  onClearPiece: () => void;
  onAnalyzeAndStyle: () => void;
}

const SEASONS: SeasonOption[] = ['Spring/Summer', 'Autumn/Winter', 'Year-Round'];
const SILHOUETTES: SilhouetteOption[] = ['Tailored', 'Relaxed', 'Sculptural'];
const BUDGETS: BudgetTierOption[] = ['Accessible', 'Contemporary', 'Designer'];

export function GarmentIntake({
  sourceImageDataUrl,
  activeGarmentLabel,
  brief,
  isAnalyzing,
  analyzeError,
  onChangeBrief,
  onUploadFile,
  onSelectSample,
  onClearPiece,
  onAnalyzeAndStyle,
}: GarmentIntakeProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      onUploadFile(file);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onUploadFile(file);
    }
    e.target.value = '';
  };

  return (
    <section className="max-w-[1280px] mx-auto px-6 md:px-10 pt-10 pb-12 border-b border-[#E5E3DC]">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-12 items-stretch">
        {/* Left Column: Editorial Headline, Upload Dropzone & Styling Brief */}
        <div className="lg:col-span-6 flex flex-col justify-between gap-8">
          <div className="space-y-4">
            <p className="text-xs font-mono tracking-widest uppercase text-[#68665F]">
              Editorial Wardrobe Studio
            </p>
            <h1 className="font-serif text-4xl md:text-5xl font-medium tracking-tight leading-[1.08] text-[#141413] text-balance">
              One Difficult Piece. Three Complete Looks.
            </h1>
            <p className="text-base text-[#68665F] leading-relaxed max-w-[54ch]">
              Upload a photograph of a garment you love but struggle to pair. Atelier extracts its
              true palette and weave, then composes Casual, Business, and Night Out flat-lay outfits
              ready to refine with natural language.
            </p>
          </div>

          {/* Upload Zone or Active Garment Controls */}
          <div className="space-y-6">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={handleFileChange}
              className="hidden"
            />

            {!sourceImageDataUrl ? (
              <div
                onDragOver={(e) => {
                  e.preventDefault();
                  setIsDragging(true);
                }}
                onDragLeave={() => setIsDragging(false)}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    fileInputRef.current?.click();
                  }
                }}
                className={`border border-dashed p-8 text-center transition-colors cursor-pointer bg-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8A6A42] ${
                  isDragging
                    ? 'border-[#141413] bg-[#F4F3EE]'
                    : 'border-[#C9C6BC] hover:border-[#141413]'
                }`}
              >
                <Upload className="w-5 h-5 mx-auto mb-3 text-[#141413]" strokeWidth={1.5} />
                <p className="text-sm font-medium text-[#141413]">
                  Drop a garment photo here, or browse from your device
                </p>
                <p className="text-xs text-[#68665F] mt-1">
                  JPG, PNG, or WebP · Clear lighting on a neutral surface works best
                </p>
              </div>
            ) : (
              <div className="border border-[#E5E3DC] bg-white p-5 flex items-center justify-between gap-4">
                <div className="flex items-center gap-4 min-w-0">
                  <div className="w-14 h-16 bg-[#F4F3EE] border border-[#E5E3DC] shrink-0 overflow-hidden">
                    <img
                      src={sourceImageDataUrl}
                      alt={activeGarmentLabel || 'Selected garment'}
                      className="w-full h-full object-cover"
                      referrerPolicy="no-referrer"
                    />
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-mono text-[#68665F]">Selected Anchor Piece</p>
                    <p className="text-sm font-medium text-[#141413] truncate">
                      {activeGarmentLabel || 'Uploaded Garment'}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="px-3 py-2 text-xs font-medium border border-[#E5E3DC] text-[#141413] hover:border-[#141413] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
                  >
                    Replace
                  </button>
                  <button
                    type="button"
                    onClick={onClearPiece}
                    aria-label="Reset selected garment"
                    className="p-2 text-[#68665F] hover:text-[#141413] border border-[#E5E3DC] hover:border-[#141413] transition-colors cursor-pointer"
                  >
                    <RotateCcw className="w-3.5 h-3.5" strokeWidth={1.5} />
                  </button>
                </div>
              </div>
            )}

            {/* Styling Brief Preferences */}
            <div className="border border-[#E5E3DC] bg-white p-5 space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
                  Styling Brief
                </span>
                <span className="text-xs text-[#68665F]">
                  {brief.season} · {brief.silhouette} · {brief.budgetTier}
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs text-[#68665F] mb-1.5">Season</label>
                  <div className="flex flex-col gap-1">
                    {SEASONS.map((season) => (
                      <button
                        key={season}
                        type="button"
                        onClick={() => onChangeBrief({ ...brief, season })}
                        className={`text-left px-3 py-1.5 text-xs border transition-colors whitespace-nowrap cursor-pointer ${
                          brief.season === season
                            ? 'bg-[#141413] text-[#FAF9F6] border-[#141413]'
                            : 'bg-[#FAF9F6] text-[#141413] border-[#E5E3DC] hover:border-[#141413]'
                        }`}
                      >
                        {season}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-xs text-[#68665F] mb-1.5">Silhouette</label>
                  <div className="flex flex-col gap-1">
                    {SILHOUETTES.map((silhouette) => (
                      <button
                        key={silhouette}
                        type="button"
                        onClick={() => onChangeBrief({ ...brief, silhouette })}
                        className={`text-left px-3 py-1.5 text-xs border transition-colors whitespace-nowrap cursor-pointer ${
                          brief.silhouette === silhouette
                            ? 'bg-[#141413] text-[#FAF9F6] border-[#141413]'
                            : 'bg-[#FAF9F6] text-[#141413] border-[#E5E3DC] hover:border-[#141413]'
                        }`}
                      >
                        {silhouette}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-xs text-[#68665F] mb-1.5">Budget Tier</label>
                  <div className="flex flex-col gap-1">
                    {BUDGETS.map((budgetTier) => (
                      <button
                        key={budgetTier}
                        type="button"
                        onClick={() => onChangeBrief({ ...brief, budgetTier })}
                        className={`text-left px-3 py-1.5 text-xs border transition-colors whitespace-nowrap cursor-pointer ${
                          brief.budgetTier === budgetTier
                            ? 'bg-[#141413] text-[#FAF9F6] border-[#141413]'
                            : 'bg-[#FAF9F6] text-[#141413] border-[#E5E3DC] hover:border-[#141413]'
                        }`}
                      >
                        {budgetTier}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {analyzeError && (
              <div
                role="alert"
                className="border border-[#9A3412]/30 bg-[#FEF2F2] px-4 py-3 text-xs text-[#7F1D1D]"
              >
                {analyzeError}
              </div>
            )}

            <button
              type="button"
              disabled={!sourceImageDataUrl || isAnalyzing}
              onClick={onAnalyzeAndStyle}
              className="w-full py-3.5 px-6 bg-[#141413] text-[#FAF9F6] text-sm font-medium flex items-center justify-center gap-2.5 hover:bg-[#2B2A28] disabled:bg-[#D8D5CC] disabled:text-[#68665F] disabled:cursor-not-allowed transition-colors cursor-pointer"
            >
              <span>{isAnalyzing ? 'Analyzing Palette & Composing Looks...' : 'Analyze & Style 3 Outfits'}</span>
              {!isAnalyzing && <ArrowRight className="w-4 h-4" strokeWidth={1.5} />}
            </button>
          </div>
        </div>

        {/* Right Column: Hero Flat-Lay Showcase or Active Garment Preview + Curated Sample Pieces */}
        <div className="lg:col-span-6 flex flex-col justify-between gap-6">
          <div className="border border-[#E5E3DC] bg-white p-4 flex-1 flex flex-col">
            <div className="relative w-full aspect-[4/3] bg-[#F4F3EE] overflow-hidden border border-[#E5E3DC]">
              <img
                src={sourceImageDataUrl || HERO_FLATLAY_IMAGE}
                alt={
                  sourceImageDataUrl
                    ? activeGarmentLabel || 'Uploaded garment preview'
                    : 'Editorial flat-lay outfit on gallery alabaster paper'
                }
                className={`w-full h-full ${sourceImageDataUrl ? 'object-contain bg-white p-4' : 'object-cover'}`}
                referrerPolicy="no-referrer"
              />
            </div>
            <div className="pt-3 flex items-center justify-between text-xs text-[#68665F]">
              <span className="font-serif italic text-sm text-[#141413]">
                {sourceImageDataUrl
                  ? activeGarmentLabel || 'Studio Input Piece'
                  : 'Fig. 01 — Overhead Flat-Lay Composition Study'}
              </span>
              <span className="font-mono">
                {sourceImageDataUrl ? 'Ready for Analysis' : 'Gallery Linen · 3 Looks'}
              </span>
            </div>
          </div>

          {/* Curated Hard-to-Style Sample Starters */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
                Or Test With a Curated Difficult Piece
              </p>
              <span className="text-xs text-[#68665F]">One-click load</span>
            </div>
            <div className="grid grid-cols-3 gap-4">
              {SAMPLE_GARMENTS.map((sample) => (
                <button
                  key={sample.id}
                  type="button"
                  onClick={() => onSelectSample(sample)}
                  className="group text-left border border-[#E5E3DC] bg-white p-2.5 hover:border-[#141413] transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8A6A42]"
                >
                  <div className="aspect-[3/4] w-full bg-[#FAF9F6] overflow-hidden mb-2 border border-[#E5E3DC]/60">
                    <img
                      src={sample.imagePath}
                      alt={sample.title}
                      className="w-full h-full object-cover group-hover:scale-[1.03] transition-transform duration-200"
                      referrerPolicy="no-referrer"
                    />
                  </div>
                  <p className="text-xs font-medium text-[#141413] leading-snug line-clamp-2">
                    {sample.title}
                  </p>
                  <p className="text-[11px] text-[#68665F] mt-0.5 truncate">{sample.subtitle}</p>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
