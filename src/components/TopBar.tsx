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
    <header className="sticky top-0 z-30 bg-[#F6F4EC] border-b-[3px] border-[#111111]">
      <div className="max-w-[1280px] mx-auto px-4 sm:px-6 md:px-10 h-[72px] flex items-center justify-between gap-4 md:gap-8">
        {/* Zone 1: Skewed Zine Masthead */}
        <button
          type="button"
          onClick={() => onSelectTab('studio')}
          className="flex items-center gap-3 whitespace-nowrap shrink-0 cursor-pointer group focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#2B4BFF]"
        >
          <span className="-rotate-2 group-hover:rotate-0 transition-transform bg-[#111111] text-[#D4FF38] font-display font-extrabold text-xl sm:text-2xl tracking-tight px-3 py-1 border-[3px] border-[#111111] shadow-[4px_4px_0_#FF3D6E]">
            ATELIER
          </span>
          <span className="hidden md:inline-block rotate-1 bg-[#D5A8FF] border-2 border-[#111111] px-2 py-0.5 font-mono text-[11px] font-bold uppercase tracking-widest text-[#111111]">
            Zine Lab
          </span>
        </button>

        {/* Zone 2: Chunky Rotated Navigation */}
        <nav className="flex items-center gap-2 sm:gap-3">
          <button
            type="button"
            onClick={() => onSelectTab('studio')}
            className={`whitespace-nowrap shrink-0 px-3 sm:px-4 py-2 text-xs font-mono font-bold uppercase tracking-wider border-[3px] border-[#111111] transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2B4BFF] ${
              activeTab === 'studio'
                ? '-rotate-1 bg-[#D4FF38] text-[#111111] shadow-[4px_4px_0_#111111]'
                : 'bg-white text-[#111111] hover:bg-[#EBE7DA] hover:-rotate-1'
            }`}
          >
            Studio
          </button>
          <button
            type="button"
            onClick={() => onSelectTab('canvas')}
            className={`whitespace-nowrap shrink-0 px-3 sm:px-4 py-2 text-xs font-mono font-bold uppercase tracking-wider border-[3px] border-[#111111] transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2B4BFF] ${
              activeTab === 'canvas'
                ? 'rotate-1 bg-[#2B4BFF] text-white shadow-[4px_4px_0_#111111]'
                : 'bg-white text-[#111111] hover:bg-[#EBE7DA] hover:rotate-1'
            }`}
          >
            Editor
          </button>
          <button
            type="button"
            onClick={() => onSelectTab('wardrobe')}
            className={`whitespace-nowrap shrink-0 px-3 sm:px-4 py-2 text-xs font-mono font-bold uppercase tracking-wider border-[3px] border-[#111111] transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#2B4BFF] ${
              activeTab === 'wardrobe'
                ? '-rotate-1 bg-[#FF3D6E] text-white shadow-[4px_4px_0_#111111]'
                : 'bg-white text-[#111111] hover:bg-[#EBE7DA] hover:-rotate-1'
            }`}
          >
            Closet{user && wardrobeCount > 0 ? ` (${wardrobeCount})` : ''}
          </button>
        </nav>

        {/* Zone 3: Single Bold Auth Trigger */}
        <div className="flex items-center shrink-0">
          {!authReady ? (
            <span className="text-xs font-mono font-bold uppercase text-[#545149] whitespace-nowrap shrink-0">
              Sync...
            </span>
          ) : user ? (
            <button
              type="button"
              onClick={onSignOut}
              title={`Signed in as ${user.email ?? user.displayName ?? 'Member'}`}
              className="zine-btn px-3.5 py-2 text-xs font-mono font-bold uppercase tracking-wider bg-white text-[#111111] whitespace-nowrap shrink-0 cursor-pointer"
            >
              Sign Out
            </button>
          ) : (
            <button
              type="button"
              onClick={onSignIn}
              className="zine-btn px-4 py-2 text-xs font-mono font-bold uppercase tracking-wider bg-[#111111] text-[#D4FF38] whitespace-nowrap shrink-0 cursor-pointer"
            >
              Sign In
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
