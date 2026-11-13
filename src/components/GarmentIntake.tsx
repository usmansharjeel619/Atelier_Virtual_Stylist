import React, { useRef, useState } from 'react';
import { Upload, Zap, RotateCcw } from 'lucide-react';
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

const SAMPLE_TILTS = ['-rotate-1', 'rotate-1', '-rotate-1'];

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
    <section className="max-w-[1280px] mx-auto px-4 sm:px-6 md:px-10 pt-10 pb-14 border-b-[3px] border-[#111111]">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-12 items-stretch">
        {/* Left Column: Collage Masthead, Dropzone & Vibe Check */}
        <div className="lg:col-span-6 flex flex-col justify-between gap-7">
          <div className="relative pt-3">
            {/* Overlapping Skewed Cutout Sticker */}
            <div className="absolute -top-1 left-5 z-10 -rotate-2 bg-[#FF3D6E] text-white border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] px-3 py-1 font-mono text-[11px] font-bold uppercase tracking-widest">
              Issue 01 · Fit Check
            </div>

            <div className="bg-[#111111] text-white border-[3px] border-[#111111] shadow-[8px_8px_0_#D4FF38] p-6 sm:p-8 pt-8 space-y-3">
              <h1 className="font-display text-4xl sm:text-5xl font-extrabold tracking-tight leading-[1.0] text-white text-balance">
                Drop a Bold Piece.{' '}
                <span className="inline-block bg-[#D4FF38] text-[#111111] px-2 -rotate-1 border-2 border-[#111111]">
                  Cook 3 Fits.
                </span>
              </h1>
              <p className="text-sm sm:text-base text-[#EBE7DA] leading-relaxed max-w-[50ch]">
                Upload any hard-to-style garment. Atelier rips its Color DNA and builds Casual,
                Business, and Night Out flat-lays ready to remix in the Editor.
              </p>
            </div>
          </div>

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
                className={`relative border-[3px] border-dashed border-[#111111] shadow-[6px_6px_0_#2B4BFF] p-7 text-center transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2B4BFF] ${
                  isDragging
                    ? 'bg-[#D4FF38] -rotate-1'
                    : 'bg-white hover:bg-[#D5A8FF]/25 hover:-translate-y-0.5'
                }`}
              >
                <div className="w-14 h-14 mx-auto mb-3 bg-[#D4FF38] border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] -rotate-3 flex items-center justify-center">
                  <Upload className="w-6 h-6 text-[#111111]" strokeWidth={2.5} />
                </div>
                <p className="font-display text-xl font-extrabold text-[#111111]">
                  Drop Your Garment Photo
                </p>
                <p className="text-xs font-mono font-bold uppercase text-[#545149] mt-1">
                  JPG · PNG · WebP — Tap or Drag
                </p>
              </div>
            ) : (
              <div className="relative zine-card-cobalt p-4 flex items-center justify-between gap-4">
                <div className="flex items-center gap-4 min-w-0">
                  <div className="w-16 h-20 -rotate-2 bg-[#EBE7DA] border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] shrink-0 overflow-hidden">
                    <img
                      src={sourceImageDataUrl}
                      alt={activeGarmentLabel || 'Selected garment'}
                      className="w-full h-full object-cover"
                      referrerPolicy="no-referrer"
                    />
                  </div>
                  <div className="min-w-0">
                    <p className="inline-block bg-[#D4FF38] border-2 border-[#111111] px-2 py-0.5 text-[10px] font-mono font-bold uppercase text-[#111111]">
                      Anchor Locked
                    </p>
                    <p className="font-display text-lg font-extrabold text-[#111111] truncate mt-1">
                      {activeGarmentLabel || 'Uploaded Garment'}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="zine-btn px-3.5 py-2 text-xs font-mono font-bold uppercase bg-white text-[#111111] hover:bg-[#D4FF38] whitespace-nowrap shrink-0 cursor-pointer"
                  >
                    Swap
                  </button>
                  <button
                    type="button"
                    onClick={onClearPiece}
                    aria-label="Reset selected garment"
                    className="zine-btn p-2 text-[#111111] bg-[#EBE7DA] hover:bg-[#FF3D6E] hover:text-white cursor-pointer"
                  >
                    <RotateCcw className="w-4 h-4" strokeWidth={2.2} />
                  </button>
                </div>
              </div>
            )}

            {/* Vibe Check Brief with Overlapping Zine Header */}
            <div className="relative pt-3">
              <div className="absolute -top-0.5 right-4 z-10 rotate-1 bg-[#2B4BFF] text-white border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] px-3 py-0.5 text-[11px] font-mono font-bold uppercase tracking-wider">
                Vibe Check Brief
              </div>

              <div className="zine-card p-5 pt-6 space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-[11px] font-mono font-bold uppercase tracking-wider text-[#545149] mb-2">
                      Season
                    </label>
                    <div className="flex flex-col gap-1.5">
                      {SEASONS.map((season) => (
                        <button
                          key={season}
                          type="button"
                          onClick={() => onChangeBrief({ ...brief, season })}
                          className={`text-left px-3 py-2 text-xs font-bold border-2 border-[#111111] transition-all whitespace-nowrap cursor-pointer ${
                            brief.season === season
                              ? '-translate-y-0.5 bg-[#D4FF38] text-[#111111] shadow-[3px_3px_0_#111111]'
                              : 'bg-[#F6F4EC] text-[#111111] hover:bg-[#EBE7DA]'
                          }`}
                        >
                          {season}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-mono font-bold uppercase tracking-wider text-[#545149] mb-2">
                      Silhouette
                    </label>
                    <div className="flex flex-col gap-1.5">
                      {SILHOUETTES.map((silhouette) => (
                        <button
                          key={silhouette}
                          type="button"
                          onClick={() => onChangeBrief({ ...brief, silhouette })}
                          className={`text-left px-3 py-2 text-xs font-bold border-2 border-[#111111] transition-all whitespace-nowrap cursor-pointer ${
                            brief.silhouette === silhouette
                              ? '-translate-y-0.5 bg-[#FF3D6E] text-white shadow-[3px_3px_0_#111111]'
                              : 'bg-[#F6F4EC] text-[#111111] hover:bg-[#EBE7DA]'
                          }`}
                        >
                          {silhouette}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-mono font-bold uppercase tracking-wider text-[#545149] mb-2">
                      Budget
                    </label>
                    <div className="flex flex-col gap-1.5">
                      {BUDGETS.map((budgetTier) => (
                        <button
                          key={budgetTier}
                          type="button"
                          onClick={() => onChangeBrief({ ...brief, budgetTier })}
                          className={`text-left px-3 py-2 text-xs font-bold border-2 border-[#111111] transition-all whitespace-nowrap cursor-pointer ${
                            brief.budgetTier === budgetTier
                              ? '-translate-y-0.5 bg-[#2B4BFF] text-white shadow-[3px_3px_0_#111111]'
                              : 'bg-[#F6F4EC] text-[#111111] hover:bg-[#EBE7DA]'
                          }`}
                        >
                          {budgetTier}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {analyzeError && (
              <div
                role="alert"
                className="border-[3px] border-[#111111] bg-[#FFE5EC] shadow-[5px_5px_0_#FF3D6E] px-4 py-3 text-xs font-bold text-[#9F1239]"
              >
                {analyzeError}
              </div>
            )}

            <button
              type="button"
              disabled={!sourceImageDataUrl || isAnalyzing}
              onClick={onAnalyzeAndStyle}
              className="zine-btn w-full py-4 px-6 bg-[#D4FF38] text-[#111111] font-display text-lg font-extrabold uppercase tracking-wide flex items-center justify-center gap-2.5 disabled:bg-[#EBE7DA] disabled:text-[#545149] disabled:shadow-none disabled:cursor-not-allowed cursor-pointer"
            >
              <Zap className="w-5 h-5 fill-current" strokeWidth={2.5} />
              <span>{isAnalyzing ? 'Cooking 3 Fits...' : 'Cook 3 Outfits'}</span>
            </button>
          </div>
        </div>

        {/* Right Column: Overlapping Zine Showcase Frame + Skewed Sample Starters */}
        <div className="lg:col-span-6 flex flex-col justify-between gap-8">
          <div className="relative pt-2">
            {/* Overlapping Top-Right Zine Tape Stamp */}
            <div className="absolute -top-1 right-6 z-10 rotate-2 bg-[#D4FF38] border-[3px] border-[#111111] shadow-[4px_4px_0_#111111] px-3 py-1 font-mono text-xs font-bold uppercase text-[#111111]">
              {sourceImageDataUrl ? 'Locked In' : '3 Occasion Drops'}
            </div>

            <div className="zine-card-lg p-4 sm:p-5 flex-1 flex flex-col bg-white">
              <div className="relative w-full aspect-[4/3] bg-[#EBE7DA] overflow-hidden border-[3px] border-[#111111]">
                <img
                  src={sourceImageDataUrl || HERO_FLATLAY_IMAGE}
                  alt={
                    sourceImageDataUrl
                      ? activeGarmentLabel || 'Uploaded garment preview'
                      : 'Editorial flat-lay outfit on chalk paper'
                  }
                  className={`w-full h-full ${sourceImageDataUrl ? 'object-contain bg-white p-4' : 'object-cover'}`}
                  referrerPolicy="no-referrer"
                />
              </div>
              <div className="pt-4 flex items-center justify-between gap-3">
                <span className="font-display font-extrabold text-base text-[#111111] truncate">
                  {sourceImageDataUrl
                    ? activeGarmentLabel || 'Studio Anchor Piece'
                    : 'Overhead Flat-Lay Fit Lab'}
                </span>
                <span className="-rotate-1 bg-[#111111] text-white px-2.5 py-0.5 font-mono text-[11px] font-bold uppercase whitespace-nowrap">
                  {sourceImageDataUrl ? 'Ready' : 'Vol. 01'}
                </span>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="-rotate-1 inline-block bg-[#D5A8FF] border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] px-3 py-1 text-xs font-mono font-bold uppercase tracking-wider text-[#111111]">
                Instant Sample Drops
              </span>
              <span className="text-xs font-mono font-bold uppercase text-[#545149]">
                Tap to load
              </span>
            </div>
            <div className="grid grid-cols-3 gap-4 sm:gap-5 pt-1">
              {SAMPLE_GARMENTS.map((sample, idx) => (
                <button
                  key={sample.id}
                  type="button"
                  onClick={() => onSelectSample(sample)}
                  className={`group zine-card ${SAMPLE_TILTS[idx % SAMPLE_TILTS.length]} hover:rotate-0 text-left p-2.5 hover:bg-[#D4FF38] transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2B4BFF]`}
                >
                  <div className="aspect-[3/4] w-full bg-[#F6F4EC] overflow-hidden mb-2.5 border-[3px] border-[#111111]">
                    <img
                      src={sample.imagePath}
                      alt={sample.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                      referrerPolicy="no-referrer"
                    />
                  </div>
                  <p className="font-display text-xs font-extrabold text-[#111111] leading-snug line-clamp-2">
                    {sample.title}
                  </p>
                  <p className="text-[11px] font-mono font-bold text-[#545149] mt-0.5 truncate">
                    {sample.subtitle}
                  </p>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
