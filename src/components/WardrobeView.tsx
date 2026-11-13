import { useEffect, useState } from 'react';
import { User } from 'firebase/auth';
import { Trash2, Download, ArrowUpRight, Zap } from 'lucide-react';
import { SavedGarmentRecord, SavedLookRecord } from '../types/stylist';
import {
  deleteSavedGarment,
  deleteSavedLook,
  subscribeToGarmentLooks,
} from '../lib/firebase';
import { triggerImageDownload } from '../lib/imageUtils';

interface WardrobeViewProps {
  user: User | null;
  garments: SavedGarmentRecord[];
  onSignIn: () => void;
  onReopenGarmentInStudio: (garment: SavedGarmentRecord, looks: SavedLookRecord[]) => void;
}

type SeasonFilter = 'All' | 'Spring' | 'Summer' | 'Autumn' | 'Winter' | 'Year-Round';

const SEASON_FILTER_OPTIONS: Array<{ value: SeasonFilter; label: string }> = [
  { value: 'All', label: 'All Seasons' },
  { value: 'Spring', label: 'Spring' },
  { value: 'Summer', label: 'Summer' },
  { value: 'Autumn', label: 'Autumn' },
  { value: 'Winter', label: 'Winter' },
  { value: 'Year-Round', label: 'Year-Round' },
];

function matchesSeasonFilter(garmentSeason: string, filter: SeasonFilter): boolean {
  if (filter === 'All') return true;
  const normalized = garmentSeason.toLowerCase();
  if (filter === 'Spring') return normalized.includes('spring');
  if (filter === 'Summer') return normalized.includes('summer');
  if (filter === 'Autumn') return normalized.includes('autumn');
  if (filter === 'Winter') return normalized.includes('winter');
  return normalized.includes('year-round');
}

export function WardrobeView({
  user,
  garments,
  onSignIn,
  onReopenGarmentInStudio,
}: WardrobeViewProps) {
  const [selectedGarmentId, setSelectedGarmentId] = useState<string | null>(null);
  const [seasonFilter, setSeasonFilter] = useState<SeasonFilter>('All');
  const [looks, setLooks] = useState<SavedLookRecord[]>([]);
  const [looksError, setLooksError] = useState<string | null>(null);

  const filteredGarments = garments.filter((g) => matchesSeasonFilter(g.season, seasonFilter));

  const activeGarment =
    filteredGarments.find((g) => g.id === selectedGarmentId) ?? filteredGarments[0] ?? null;

  useEffect(() => {
    if (!user || !activeGarment) {
      setLooks([]);
      return;
    }

    setLooksError(null);
    const unsub = subscribeToGarmentLooks(
      user.uid,
      activeGarment.id,
      (items) => setLooks(items),
      (err) => setLooksError(err.message)
    );
    return () => unsub();
  }, [user, activeGarment?.id]);

  if (!user) {
    return (
      <section className="max-w-[780px] mx-auto px-4 sm:px-6 md:px-10 py-16">
        <div className="relative pt-3">
          <span className="absolute -top-0.5 left-1/2 -translate-x-1/2 z-10 -rotate-2 bg-[#2B4BFF] text-white zine-sticker px-4 py-1 text-xs font-mono font-bold uppercase tracking-wider">
            Cloud Closet Sync
          </span>

          <div className="bg-white border-[4px] border-[#111111] shadow-[10px_10px_0_#FF3D6E] p-8 sm:p-12 pt-11 space-y-5 text-center">
            <h1 className="font-display text-3xl sm:text-5xl font-extrabold text-[#111111] text-balance leading-[1.05]">
              Unlock Your Saved Fit Archive
            </h1>
            <p className="text-sm sm:text-base font-medium text-[#545149] max-w-[48ch] mx-auto leading-relaxed">
              Sign in with Google to save analyzed garments, Color DNA palettes, and Casual,
              Business, and Night Out flat-lays across web and Android.
            </p>
            <div className="pt-2">
              <button
                type="button"
                onClick={onSignIn}
                className="zine-btn px-8 py-4 bg-[#D4FF38] text-[#111111] font-display text-base font-extrabold uppercase tracking-wide inline-flex items-center gap-2 cursor-pointer"
              >
                <Zap className="w-4 h-4 fill-current" strokeWidth={2.5} />
                <span>Sign In with Google</span>
              </button>
            </div>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="max-w-[1280px] mx-auto px-4 sm:px-6 md:px-10 py-10 space-y-8">
      <div className="border-b-[3px] border-[#111111] pb-6 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-2">
          <span className="inline-block -rotate-1 bg-[#D4FF38] text-[#111111] zine-sticker px-3 py-0.5 text-xs font-mono font-bold uppercase tracking-wider">
            Closet · {user.email}
          </span>
          <h1 className="font-display text-3xl md:text-5xl font-extrabold text-[#111111]">
            Your Cloud Closet
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2.5">
            <label
              htmlFor="wardrobe-season-filter"
              className="text-xs font-mono font-bold uppercase tracking-wider text-[#545149] whitespace-nowrap"
            >
              Season
            </label>
            <select
              id="wardrobe-season-filter"
              value={seasonFilter}
              onChange={(e) => setSeasonFilter(e.target.value as SeasonFilter)}
              className="border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] bg-white px-3 py-2 text-xs font-mono font-bold text-[#111111] focus:outline-none cursor-pointer"
            >
              {SEASON_FILTER_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <span className="bg-[#111111] text-[#D4FF38] border-2 border-[#111111] px-2.5 py-1 text-xs font-mono font-bold tabular-nums whitespace-nowrap">
            {seasonFilter === 'All'
              ? `${garments.length} ${garments.length === 1 ? 'Anchor Piece' : 'Anchor Pieces'}`
              : `${filteredGarments.length} of ${garments.length} ${
                  garments.length === 1 ? 'Piece' : 'Pieces'
                }`}
          </span>
        </div>
      </div>

      {garments.length === 0 ? (
        <div className="zine-card-lime p-12 text-center space-y-3">
          <p className="font-display text-2xl font-extrabold text-[#111111]">
            Your Closet Is Empty
          </p>
          <p className="text-sm font-medium text-[#545149] max-w-md mx-auto">
            Cook 3 outfits in Studio or remix a custom look in the Editor, then click Save to store
            it in your Cloud Closet.
          </p>
        </div>
      ) : filteredGarments.length === 0 ? (
        <div className="zine-card-coral p-12 text-center space-y-4">
          <p className="font-display text-2xl font-extrabold text-[#111111]">
            No Fits Saved for {seasonFilter}
          </p>
          <p className="text-sm font-medium text-[#545149] max-w-md mx-auto">
            None of your saved garments are tagged for {seasonFilter}.
          </p>
          <button
            type="button"
            onClick={() => setSeasonFilter('All')}
            className="zine-btn px-5 py-2.5 bg-[#D4FF38] text-xs font-mono font-bold uppercase text-[#111111] cursor-pointer"
          >
            Show All Seasons
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          <div className="lg:col-span-4 space-y-4">
            <span className="inline-block -rotate-1 bg-[#D5A8FF] border-2 border-[#111111] px-2.5 py-0.5 text-xs font-mono font-bold uppercase tracking-wider text-[#111111]">
              Saved Pieces
            </span>
            <div className="space-y-3.5">
              {filteredGarments.map((garment, idx) => {
                const isSelected = activeGarment?.id === garment.id;
                const tilt = idx % 2 === 0 ? '-rotate-1' : 'rotate-1';
                return (
                  <div
                    key={garment.id}
                    className={`${
                      isSelected ? '-rotate-1 bg-[#D4FF38] translate-x-1' : `${tilt} bg-white hover:bg-[#EBE7DA]`
                    } hover:rotate-0 border-[3px] border-[#111111] shadow-[4px_4px_0_#111111] p-3 flex items-center justify-between gap-3 transition-all`}
                  >
                    <button
                      type="button"
                      onClick={() => setSelectedGarmentId(garment.id)}
                      className="flex items-center gap-3 min-w-0 flex-1 text-left cursor-pointer"
                    >
                      <div className="w-14 h-16 bg-[#EBE7DA] border-[3px] border-[#111111] shrink-0 overflow-hidden">
                        <img
                          src={garment.sourceImageDataUrl}
                          alt={garment.title}
                          className="w-full h-full object-cover"
                          referrerPolicy="no-referrer"
                        />
                      </div>
                      <div className="min-w-0">
                        <p className="font-display text-sm font-extrabold text-[#111111] truncate">
                          {garment.title}
                        </p>
                        <p className="text-xs font-mono font-bold text-[#545149] truncate mt-0.5">
                          {garment.category} · {garment.season}
                        </p>
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => deleteSavedGarment(user.uid, garment.id)}
                      aria-label={`Delete ${garment.title}`}
                      className="p-2 border-2 border-transparent hover:border-[#111111] hover:bg-[#FF3D6E] text-[#111111] hover:text-white transition-colors cursor-pointer shrink-0"
                    >
                      <Trash2 className="w-4 h-4" strokeWidth={2.2} />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {activeGarment && (
            <div className="lg:col-span-8 space-y-7">
              <div className="relative pt-3">
                <div className="absolute -top-0.5 left-5 z-10 -rotate-1 bg-[#FF6B2C] text-white zine-sticker px-3 py-0.5 text-[11px] font-mono font-bold uppercase">
                  {activeGarment.category} · {activeGarment.season}
                </div>

                <div className="zine-card-cobalt p-6 pt-7 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="space-y-1.5">
                    <p className="text-xs font-mono font-bold uppercase text-[#2B4BFF]">
                      {activeGarment.silhouette} · {activeGarment.budgetTier}
                    </p>
                    <h2 className="font-display text-2xl sm:text-3xl font-extrabold text-[#111111]">
                      {activeGarment.title}
                    </h2>
                    <p className="text-xs sm:text-sm font-medium text-[#545149] max-w-[60ch] leading-relaxed">
                      {activeGarment.styleSummary}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => onReopenGarmentInStudio(activeGarment, looks)}
                    className="zine-btn px-4 py-2.5 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
                  >
                    <span>Reopen in Studio</span>
                    <ArrowUpRight className="w-3.5 h-3.5" strokeWidth={2.5} />
                  </button>
                </div>
              </div>

              {looksError && (
                <div className="border-[3px] border-[#111111] bg-[#FFE5EC] shadow-[4px_4px_0_#FF3D6E] p-4 text-xs font-bold text-[#9F1239]">
                  {looksError}
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-7">
                {looks.map((look, lIdx) => {
                  const isEven = lIdx % 2 === 0;
                  return (
                    <div key={look.id} className="relative pt-3 flex">
                      <span
                        className={`absolute -top-0.5 left-4 z-10 ${
                          isEven
                            ? '-rotate-2 bg-[#111111] text-[#D4FF38]'
                            : 'rotate-2 bg-[#FF3D6E] text-white'
                        } zine-sticker px-3 py-0.5 text-xs font-mono font-bold uppercase`}
                      >
                        {look.occasion}
                      </span>

                      <article
                        className={`${
                          isEven ? 'zine-card-lime' : 'zine-card-coral'
                        } flex-1 flex flex-col overflow-hidden`}
                      >
                        {look.flatLayImageDataUrl && (
                          <div className="aspect-[3/4] w-full bg-[#EBE7DA] border-b-[3px] border-[#111111] overflow-hidden">
                            <img
                              src={look.flatLayImageDataUrl}
                              alt={look.title}
                              className="w-full h-full object-cover"
                              referrerPolicy="no-referrer"
                            />
                          </div>
                        )}
                        <div className="p-5 flex-1 flex flex-col justify-between gap-4">
                          <div>
                            <h3 className="font-display text-xl font-extrabold text-[#111111] mt-0.5">
                              {look.title}
                            </h3>
                            <p className="text-xs font-medium text-[#545149] mt-1.5 leading-relaxed">
                              {look.stylingRationale}
                            </p>
                          </div>

                          <div className="flex items-center justify-between pt-3 border-t-[3px] border-[#111111]/15">
                            <span className="bg-[#EBE7DA] border-2 border-[#111111] px-2 py-0.5 text-[11px] font-mono font-bold text-[#111111]">
                              {look.pieces.length} PIECES
                            </span>
                            <div className="flex items-center gap-2">
                              {look.flatLayImageDataUrl && (
                                <button
                                  type="button"
                                  onClick={() =>
                                    triggerImageDownload(
                                      look.flatLayImageDataUrl!,
                                      `${look.title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}.jpg`
                                    )
                                  }
                                  aria-label={`Download ${look.title}`}
                                  className="zine-btn p-2 bg-[#F6F4EC] text-[#111111] hover:bg-[#D4FF38] cursor-pointer"
                                >
                                  <Download className="w-3.5 h-3.5" strokeWidth={2.2} />
                                </button>
                              )}
                              <button
                                type="button"
                                onClick={() => deleteSavedLook(user.uid, activeGarment.id, look.id)}
                                aria-label={`Delete ${look.title}`}
                                className="zine-btn p-2 bg-[#F6F4EC] text-[#111111] hover:bg-[#FF3D6E] hover:text-white cursor-pointer"
                              >
                                <Trash2 className="w-3.5 h-3.5" strokeWidth={2.2} />
                              </button>
                            </div>
                          </div>
                        </div>
                      </article>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
