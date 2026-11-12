import { User } from 'firebase/auth';

export type ActiveTab = 'studio' | 'canvas' | 'wardrobe';

interface TopBarProps {
  activeTab: ActiveTab;
  onSelectTab: (tab: ActiveTab) => void;
  user: User | null;
  authReady: boolean;
  wardrobeCount: number;
  onSignIn: () => void;
  onSignOut: () => void;
}

export function TopBar({
  activeTab,
  onSelectTab,
  user,
  authReady,
  wardrobeCount,
  onSignIn,
  onSignOut,
}: TopBarProps) {
  return (
    <header className="sticky top-0 z-30 bg-[#FAF9F6]/95 backdrop-blur-sm border-b border-[#E5E3DC]">
      <div className="max-w-[1280px] mx-auto px-6 md:px-10 h-16 flex items-center justify-between gap-8">
        {/* Zone 1: Single-line editorial wordmark */}
        <button
          type="button"
          onClick={() => onSelectTab('studio')}
          className="font-serif text-2xl font-semibold tracking-tight text-[#141413] whitespace-nowrap shrink-0 cursor-pointer hover:opacity-80 transition-opacity focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#8A6A42]"
        >
          Atelier
        </button>

        {/* Zone 2: Concise single-line text navigation */}
        <nav className="flex items-center gap-6 md:gap-8 text-sm font-medium text-[#68665F]">
          <button
            type="button"
            onClick={() => onSelectTab('studio')}
            className={`whitespace-nowrap shrink-0 py-1 border-b transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#8A6A42] ${
              activeTab === 'studio'
                ? 'text-[#141413] border-[#141413]'
                : 'border-transparent hover:text-[#141413]'
            }`}
          >
            Styling Studio
          </button>
          <button
            type="button"
            onClick={() => onSelectTab('canvas')}
            className={`whitespace-nowrap shrink-0 py-1 border-b transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#8A6A42] ${
              activeTab === 'canvas'
                ? 'text-[#141413] border-[#141413]'
                : 'border-transparent hover:text-[#141413]'
            }`}
          >
            Flat-Lay Canvas
          </button>
          <button
            type="button"
            onClick={() => onSelectTab('wardrobe')}
            className={`whitespace-nowrap shrink-0 py-1 border-b transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#8A6A42] ${
              activeTab === 'wardrobe'
                ? 'text-[#141413] border-[#141413]'
                : 'border-transparent hover:text-[#141413]'
            }`}
          >
            Wardrobe{user && wardrobeCount > 0 ? ` (${wardrobeCount})` : ''}
          </button>
        </nav>

        {/* Zone 3: Single primary auth action */}
        <div className="flex items-center shrink-0">
          {!authReady ? (
            <span className="text-xs text-[#68665F] whitespace-nowrap shrink-0">Loading</span>
          ) : user ? (
            <button
              type="button"
              onClick={onSignOut}
              title={`Signed in as ${user.email ?? user.displayName ?? 'Member'}`}
              className="px-4 py-2 text-xs font-medium border border-[#E5E3DC] bg-white text-[#141413] hover:border-[#141413] transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8A6A42]"
            >
              Sign Out
            </button>
          ) : (
            <button
              type="button"
              onClick={onSignIn}
              className="px-4 py-2 text-xs font-medium bg-[#141413] text-[#FAF9F6] hover:bg-[#2B2A28] transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8A6A42]"
            >
              Sign In
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
