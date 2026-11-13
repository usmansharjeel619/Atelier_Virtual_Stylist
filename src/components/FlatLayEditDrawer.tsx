import { useState } from 'react';
import { X, Zap, Download, RotateCcw } from 'lucide-react';
import { GeneratedLookState } from '../types/stylist';
import { splitDataUrl, triggerImageDownload } from '../lib/imageUtils';

interface FlatLayEditDrawerProps {
  look: GeneratedLookState | null;
  sourceGarmentDataUrl: string | null;
  onClose: () => void;
  onUpdateLook: (updated: GeneratedLookState) => void;
}

const SUGGESTED_PROMPTS = [
  'Swap shoes for chunky silver sneakers',
  'Add an oversized vintage leather jacket beside the look',
  'Replace the bag with a metallic silver mini baguette',
  'Add oval tinted sunglasses and chunky chrome rings',
];

export function FlatLayEditDrawer({
  look,
  sourceGarmentDataUrl,
  onClose,
  onUpdateLook,
}: FlatLayEditDrawerProps) {
  const [prompt, setPrompt] = useState('');
  const [aspectRatio, setAspectRatio] = useState<'3:4' | '1:1' | '4:3' | '9:16'>('3:4');
  const [imageSize, setImageSize] = useState<'1K' | '2K'>('1K');
  const [isEditing, setIsEditing] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  if (!look) return null;

  const handleApplyPromptEdit = async () => {
    const trimmed = prompt.trim();
    if (!trimmed) return;

    setIsEditing(true);
    setEditError(null);

    try {
      const payload: Record<string, unknown> = {
        editPrompt: trimmed,
        aspectRatio,
        imageSize,
      };

      if (look.imageDataUrl) {
        const base = splitDataUrl(look.imageDataUrl);
        payload.baseImageBase64 = base.base64;
        payload.baseMimeType = base.mimeType;
      }

      if (sourceGarmentDataUrl) {
        const anchor = splitDataUrl(sourceGarmentDataUrl);
        payload.sourceGarmentBase64 = anchor.base64;
        payload.sourceGarmentMimeType = anchor.mimeType;
      }

      const res = await fetch('/api/stylist/edit-flatlay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Failed to refine flat-lay image.');
      }

      const nextHistory = [...look.imageHistory, data.imageDataUrl];
      const nextPrompts = [...look.editPromptHistory, trimmed];

      onUpdateLook({
        ...look,
        imageDataUrl: data.imageDataUrl,
        imageHistory: nextHistory,
        editPromptHistory: nextPrompts,
        error: null,
      });

      setPrompt('');
    } catch (err) {
      setEditError(err instanceof Error ? err.message : 'Unable to apply flat-lay edit.');
    } finally {
      setIsEditing(false);
    }
  };

  const handleSelectVersion = (imgUrl: string) => {
    onUpdateLook({
      ...look,
      imageDataUrl: imgUrl,
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-[2px]"
      role="dialog"
      aria-modal="true"
      aria-label={`Refine ${look.title}`}
    >
      <div className="w-full max-w-2xl zine-bg-grid h-full overflow-y-auto border-l-[4px] border-[#111111] flex flex-col">
        <div className="px-6 py-5 bg-[#111111] text-white border-b-[3px] border-[#111111] flex items-center justify-between sticky top-0 z-20 gap-4">
          <div className="space-y-1.5 min-w-0">
            <span className="inline-block -rotate-1 bg-[#D4FF38] text-[#111111] border-2 border-white px-2.5 py-0.5 text-[11px] font-mono font-bold uppercase tracking-wider">
              Quick Fit Tweak · {look.occasion}
            </span>
            <h3 className="font-display text-2xl font-extrabold text-white truncate">
              {look.title}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close flat-lay editor"
            className="p-2 border-[3px] border-white text-white hover:bg-[#FF3D6E] hover:border-[#FF3D6E] transition-colors cursor-pointer shrink-0"
          >
            <X className="w-4 h-4" strokeWidth={2.5} />
          </button>
        </div>

        <div className="p-6 space-y-6 flex-1">
          <div className="grid grid-cols-1 sm:grid-cols-12 gap-6 items-start">
            <div className="sm:col-span-8 relative pt-2">
              <div className="absolute -top-0.5 right-4 z-10 rotate-2 bg-[#D5A8FF] text-[#111111] zine-sticker px-2.5 py-0.5 text-[11px] font-mono font-bold uppercase">
                {look.imageHistory.length > 1
                  ? `Version ${look.imageHistory.length}`
                  : 'Original Drop'}
              </div>

              <div className="zine-card-lg p-3">
                <div className="aspect-[3/4] w-full bg-[#EBE7DA] border-[3px] border-[#111111] overflow-hidden relative">
                  {isEditing ? (
                    <div className="w-full h-full flex flex-col items-center justify-center p-6 text-center">
                      <div className="w-10 h-10 border-[3px] border-[#111111] border-t-[#D4FF38] rounded-full animate-spin mb-3" />
                      <p className="font-display text-lg font-extrabold text-[#111111]">
                        Rendering Remix
                      </p>
                    </div>
                  ) : look.imageDataUrl ? (
                    <img
                      src={look.imageDataUrl}
                      alt={look.title}
                      className="w-full h-full object-cover"
                      referrerPolicy="no-referrer"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center p-6 text-center text-xs font-mono font-bold text-[#545149]">
                      No base image yet — drop a prompt below to generate one directly.
                    </div>
                  )}
                </div>
                {look.imageDataUrl && !isEditing && (
                  <div className="pt-3 flex items-center justify-between text-xs font-mono">
                    <span className="font-bold uppercase text-[#111111]">
                      Live Preview
                    </span>
                    <button
                      type="button"
                      onClick={() =>
                        triggerImageDownload(
                          look.imageDataUrl!,
                          `atelier-${look.occasion.toLowerCase()}-edited.png`
                        )
                      }
                      className="inline-flex items-center gap-1.5 font-bold text-[#2B4BFF] hover:underline cursor-pointer"
                    >
                      <Download className="w-3.5 h-3.5" strokeWidth={2.5} />
                      <span>Download PNG</span>
                    </button>
                  </div>
                )}
              </div>
            </div>

            <div className="sm:col-span-4 space-y-5">
              {sourceGarmentDataUrl && (
                <div className="relative pt-2">
                  <div className="absolute -top-0.5 left-3 z-10 -rotate-2 bg-[#2B4BFF] text-white zine-sticker px-2 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider">
                    Anchor Piece
                  </div>
                  <div className="zine-card-cobalt -rotate-1 hover:rotate-0 transition-transform p-3 pt-4">
                    <div className="aspect-[3/4] w-full bg-[#F6F4EC] border-[3px] border-[#111111] overflow-hidden">
                      <img
                        src={sourceGarmentDataUrl}
                        alt="Anchor garment reference"
                        className="w-full h-full object-contain p-1.5"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                  </div>
                </div>
              )}

              {look.imageHistory.length > 1 && (
                <div className="relative pt-2">
                  <div className="absolute -top-0.5 left-3 z-10 rotate-1 bg-[#FF3D6E] text-white zine-sticker px-2 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider">
                    Version Stack
                  </div>
                  <div className="zine-card-coral p-3 pt-5 space-y-2">
                    <div className="grid grid-cols-3 gap-2">
                      {look.imageHistory.map((histUrl, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => handleSelectVersion(histUrl)}
                          className={`aspect-square border-[3px] overflow-hidden cursor-pointer transition-transform ${
                            look.imageDataUrl === histUrl
                              ? 'border-[#2B4BFF] ring-2 ring-[#D4FF38] -rotate-2 scale-105'
                              : 'border-[#111111] opacity-75 hover:opacity-100'
                          }`}
                          title={`Restore version ${idx + 1}`}
                        >
                          <img
                            src={histUrl}
                            alt={`Version ${idx + 1}`}
                            className="w-full h-full object-cover"
                            referrerPolicy="no-referrer"
                          />
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="relative pt-3">
            <div className="absolute -top-0.5 left-5 z-10 -rotate-1 bg-[#D4FF38] text-[#111111] zine-sticker px-3 py-0.5 text-xs font-mono font-bold uppercase tracking-wider">
              Remix Prompt Controls
            </div>

            <div className="zine-card p-5 pt-7 space-y-4">
              <label
                htmlFor="flatlay-edit-prompt"
                className="block text-xs font-mono font-bold uppercase tracking-wider text-[#2B4BFF]"
              >
                Tell the Stylist What to Swap
              </label>
              <textarea
                id="flatlay-edit-prompt"
                rows={3}
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="e.g., Swap the shoes for chunky silver runners and add an oversized leather jacket..."
                className="w-full border-[3px] border-[#111111] bg-[#F6F4EC] p-3 text-sm font-medium text-[#111111] placeholder:text-[#545149]/70 focus:outline-none focus:bg-white"
              />

              <div className="space-y-2">
                <p className="text-xs font-mono font-bold uppercase text-[#545149]">
                  One-Tap Remix Moves
                </p>
                <div className="flex flex-wrap gap-2">
                  {SUGGESTED_PROMPTS.map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      onClick={() => setPrompt(suggestion)}
                      className={`px-3 py-1.5 text-xs font-bold border-2 border-[#111111] transition-all text-left cursor-pointer ${
                        prompt === suggestion
                          ? '-translate-y-0.5 bg-[#D4FF38] text-[#111111] shadow-[3px_3px_0_#111111]'
                          : 'bg-[#F6F4EC] text-[#111111] hover:bg-[#EBE7DA]'
                      }`}
                    >
                      {suggestion}
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 pt-3 border-t-[3px] border-[#111111]/15">
                <div>
                  <p className="text-xs font-mono font-bold uppercase text-[#545149] mb-1.5">
                    Aspect Ratio
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {(['3:4', '1:1', '4:3', '9:16'] as const).map((ratio) => (
                      <button
                        key={ratio}
                        type="button"
                        onClick={() => setAspectRatio(ratio)}
                        className={`px-2.5 py-1.5 text-xs font-mono font-bold border-2 border-[#111111] cursor-pointer ${
                          aspectRatio === ratio
                            ? 'bg-[#D4FF38] text-[#111111] shadow-[2px_2px_0_#111111]'
                            : 'bg-[#F6F4EC] text-[#111111]'
                        }`}
                      >
                        {ratio}
                      </button>
                    ))}
                  </div>
                </div>
                <div>
                  <p className="text-xs font-mono font-bold uppercase text-[#545149] mb-1.5">
                    Resolution
                  </p>
                  <div className="flex gap-1.5">
                    {(['1K', '2K'] as const).map((sz) => (
                      <button
                        key={sz}
                        type="button"
                        onClick={() => setImageSize(sz)}
                        className={`px-3 py-1.5 text-xs font-mono font-bold border-2 border-[#111111] cursor-pointer ${
                          imageSize === sz
                            ? 'bg-[#2B4BFF] text-white shadow-[2px_2px_0_#111111]'
                            : 'bg-[#F6F4EC] text-[#111111]'
                        }`}
                      >
                        {sz}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {editError && (
                <div
                  role="alert"
                  className="border-[3px] border-[#111111] bg-[#FFE5EC] shadow-[4px_4px_0_#FF3D6E] px-4 py-3 text-xs font-bold text-[#9F1239]"
                >
                  {editError}
                </div>
              )}

              <div className="flex items-center gap-3 pt-2">
                <button
                  type="button"
                  disabled={!prompt.trim() || isEditing}
                  onClick={handleApplyPromptEdit}
                  className="zine-btn flex-1 py-3 px-5 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase flex items-center justify-center gap-2 disabled:bg-[#EBE7DA] disabled:text-[#545149] disabled:shadow-none disabled:cursor-not-allowed cursor-pointer"
                >
                  <Zap className="w-3.5 h-3.5 fill-current" strokeWidth={2.5} />
                  <span>{isEditing ? 'Remixing...' : 'Apply Remix'}</span>
                </button>

                {look.imageHistory.length > 1 && (
                  <button
                    type="button"
                    onClick={() => handleSelectVersion(look.imageHistory[0])}
                    className="zine-btn py-3 px-4 bg-white text-xs font-mono font-bold uppercase text-[#111111] hover:bg-[#EBE7DA] flex items-center gap-1.5 cursor-pointer"
                  >
                    <RotateCcw className="w-3.5 h-3.5" strokeWidth={2.2} />
                    <span>Revert</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          {look.editPromptHistory.length > 0 && (
            <div className="zine-card-lime p-5 space-y-2.5">
              <span className="inline-block bg-[#111111] text-[#D4FF38] px-2.5 py-0.5 text-xs font-mono font-bold uppercase tracking-wider">
                Remix Log
              </span>
              <ol className="space-y-1.5 text-xs font-medium text-[#111111] list-decimal list-inside">
                {look.editPromptHistory.map((item, idx) => (
                  <li key={idx} className="leading-relaxed">
                    {item}
                  </li>
                ))}
              </ol>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
