import { useEffect, useState } from 'react';
import { User } from 'firebase/auth';
import { Trash2, Download, ArrowUpRight } from 'lucide-react';
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
      <section className="max-w-[960px] mx-auto px-6 md:px-10 py-20 text-center space-y-6">
        <p className="text-xs font-mono uppercase tracking-wider text-[#8A6A42]">
          Cloud Wardrobe Sync
        </p>
        <h1 className="font-serif text-4xl font-medium text-[#141413] text-balance">
          Sign In to Access Your Saved Garments &amp; Flat-Lay Archive
        </h1>
        <p className="text-sm text-[#68665F] max-w-[48ch] mx-auto leading-relaxed">
          Connect your Google account to save analyzed wardrobe pieces, store your Casual,
          Business, and Night Out flat-lay looks, and revisit prompt edits from any device.
        </p>
        <div>
          <button
            type="button"
            onClick={onSignIn}
            className="px-6 py-3 bg-[#141413] text-[#FAF9F6] text-sm font-medium hover:bg-[#2B2A28] transition-colors cursor-pointer"
          >
            Sign In with Google
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="max-w-[1280px] mx-auto px-6 md:px-10 py-10 space-y-10">
      <div className="border-b border-[#E5E3DC] pb-6 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <p className="text-xs font-mono uppercase tracking-wider text-[#8A6A42]">
            Synced Archive · {user.email}
          </p>
          <h1 className="font-serif text-3xl md:text-4xl font-medium text-[#141413] mt-1">
            Personal Cloud Wardrobe
          </h1>
        </div>

        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2.5">
            <label
              htmlFor="wardrobe-season-filter"
              className="text-xs font-mono uppercase tracking-wider text-[#68665F] whitespace-nowrap"
            >
              Season
            </label>
            <select
              id="wardrobe-season-filter"
              value={seasonFilter}
              onChange={(e) => setSeasonFilter(e.target.value as SeasonFilter)}
              className="border border-[#E5E3DC] bg-white px-3 py-2 text-xs font-medium text-[#141413] hover:border-[#141413] focus:outline-none focus:border-[#141413] cursor-pointer"
            >
              {SEASON_FILTER_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <p className="text-xs font-mono tabular-nums text-[#68665F] whitespace-nowrap">
            {seasonFilter === 'All'
              ? `${garments.length} ${garments.length === 1 ? 'Anchor Piece' : 'Anchor Pieces'}`
              : `${filteredGarments.length} of ${garments.length} ${
                  garments.length === 1 ? 'Piece' : 'Pieces'
                }`}
          </p>
        </div>
      </div>

      {garments.length === 0 ? (
        <div className="border border-[#E5E3DC] bg-white p-12 text-center space-y-3">
          <p className="font-serif text-2xl text-[#141413]">Your Archive Is Empty</p>
          <p className="text-sm text-[#68665F] max-w-md mx-auto">
            Style a garment in the Studio or compose a look on the Flat-Lay Canvas, then click Save
            to store it in your cloud wardrobe.
          </p>
        </div>
      ) : filteredGarments.length === 0 ? (
        <div className="border border-[#E5E3DC] bg-white p-12 text-center space-y-4">
          <p className="font-serif text-2xl text-[#141413]">
            No Saved Pieces for {seasonFilter}
          </p>
          <p className="text-sm text-[#68665F] max-w-md mx-auto">
            None of your saved garments are tagged for {seasonFilter}. Switch seasons or reset the
            filter to view your full archive.
          </p>
          <button
            type="button"
            onClick={() => setSeasonFilter('All')}
            className="px-4 py-2 border border-[#141413] bg-white text-xs font-medium text-[#141413] hover:bg-[#141413] hover:text-[#FAF9F6] transition-colors cursor-pointer"
          >
            Show All Seasons
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Rail: Saved Anchor Pieces */}
          <div className="lg:col-span-4 space-y-3">
            <p className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
              Saved Pieces
            </p>
            <div className="space-y-3">
              {filteredGarments.map((garment) => {
                const isSelected = activeGarment?.id === garment.id;
                return (
                  <div
                    key={garment.id}
                    className={`border bg-white p-3 flex items-center justify-between gap-3 transition-colors ${
                      isSelected ? 'border-[#141413]' : 'border-[#E5E3DC] hover:border-[#141413]/60'
                    }`}
                  >
                    <button
                      type="button"
                      onClick={() => setSelectedGarmentId(garment.id)}
                      className="flex items-center gap-3 min-w-0 flex-1 text-left cursor-pointer"
                    >
                      <div className="w-14 h-16 bg-[#F4F3EE] border border-[#E5E3DC] shrink-0 overflow-hidden">
                        <img
                          src={garment.sourceImageDataUrl}
                          alt={garment.title}
                          className="w-full h-full object-cover"
                          referrerPolicy="no-referrer"
                        />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-[#141413] truncate">
                          {garment.title}
                        </p>
                        <p className="text-xs text-[#68665F] truncate mt-0.5">
                          {garment.category} · {garment.season}
                        </p>
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => deleteSavedGarment(user.uid, garment.id)}
                      aria-label={`Delete ${garment.title}`}
                      className="p-2 text-[#68665F] hover:text-[#9A3412] transition-colors cursor-pointer shrink-0"
                    >
                      <Trash2 className="w-4 h-4" strokeWidth={1.5} />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right Pane: Selected Garment Dossier & Saved Looks */}
          {activeGarment && (
            <div className="lg:col-span-8 space-y-6">
              <div className="border border-[#E5E3DC] bg-white p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="space-y-1">
                  <p className="text-xs text-[#68665F]">
                    {activeGarment.category} · {activeGarment.season} · {activeGarment.silhouette} ·{' '}
                    {activeGarment.budgetTier}
                  </p>
                  <h2 className="font-serif text-2xl font-medium text-[#141413]">
                    {activeGarment.title}
                  </h2>
                  <p className="text-xs text-[#68665F] max-w-[60ch] leading-relaxed">
                    {activeGarment.styleSummary}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => onReopenGarmentInStudio(activeGarment, looks)}
                  className="px-4 py-2.5 bg-[#141413] text-[#FAF9F6] text-xs font-medium inline-flex items-center gap-1.5 hover:bg-[#2B2A28] whitespace-nowrap shrink-0 cursor-pointer"
                >
                  <span>Open in Studio</span>
                  <ArrowUpRight className="w-3.5 h-3.5" strokeWidth={1.5} />
                </button>
              </div>

              {looksError && (
                <div className="border border-[#9A3412]/30 bg-[#FEF2F2] p-4 text-xs text-[#7F1D1D]">
                  {looksError}
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {looks.map((look) => (
                  <article
                    key={look.id}
                    className="border border-[#E5E3DC] bg-white flex flex-col"
                  >
                    {look.flatLayImageDataUrl && (
                      <div className="aspect-[3/4] w-full bg-[#F4F3EE] border-b border-[#E5E3DC] overflow-hidden">
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
                        <p className="text-xs font-mono text-[#8A6A42]">{look.occasion}</p>
                        <h3 className="font-serif text-xl font-medium text-[#141413] mt-0.5">
                          {look.title}
                        </h3>
                        <p className="text-xs text-[#68665F] mt-1.5 leading-relaxed">
                          {look.stylingRationale}
                        </p>
                      </div>

                      <div className="flex items-center justify-between pt-3 border-t border-[#E5E3DC]">
                        <span className="text-[11px] font-mono text-[#68665F]">
                          {look.pieces.length} pieces
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
                              className="p-2 border border-[#E5E3DC] text-[#141413] hover:border-[#141413] cursor-pointer"
                            >
                              <Download className="w-3.5 h-3.5" strokeWidth={1.5} />
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => deleteSavedLook(user.uid, activeGarment.id, look.id)}
                            aria-label={`Delete ${look.title}`}
                            className="p-2 border border-[#E5E3DC] text-[#68665F] hover:text-[#9A3412] hover:border-[#9A3412] cursor-pointer"
                          >
                            <Trash2 className="w-3.5 h-3.5" strokeWidth={1.5} />
                          </button>
                        </div>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
