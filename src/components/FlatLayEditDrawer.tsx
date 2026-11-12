import { useState } from 'react';
import { X, Wand2, Download, RotateCcw } from 'lucide-react';
import { GeneratedLookState } from '../types/stylist';
import { splitDataUrl, triggerImageDownload } from '../lib/imageUtils';

interface FlatLayEditDrawerProps {
  look: GeneratedLookState | null;
  sourceGarmentDataUrl: string | null;
  onClose: () => void;
  onUpdateLook: (updated: GeneratedLookState) => void;
}

const SUGGESTED_PROMPTS = [
  'Swap footwear for minimalist white leather sneakers',
  'Add a structured camel wool trench coat folded neatly beside the look',
  'Replace the bag with an espresso woven leather tote',
  'Add sculptural brushed gold hoop earrings and tortoiseshell sunglasses',
];

export function FlatLayEditDrawer({
  look,
  sourceGarmentDataUrl,
  onClose,
  onUpdateLook,
}: FlatLayEditDrawerProps) {
  const [prompt, setPrompt] = useState('');
  const [aspectRatio, setAspectRatio] = useState<'3:4' | '1:1' | '4:3'>('3:4');
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
      className="fixed inset-0 z-50 flex justify-end bg-black/40 backdrop-blur-[2px]"
      role="dialog"
      aria-modal="true"
      aria-label={`Refine ${look.title}`}
    >
      <div className="w-full max-w-2xl bg-[#FAF9F6] h-full overflow-y-auto border-l border-[#E5E3DC] flex flex-col">
        {/* Drawer Header */}
        <div className="px-6 py-5 bg-white border-b border-[#E5E3DC] flex items-center justify-between sticky top-0 z-10">
          <div>
            <p className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
              Prompt Flat-Lay Editor · {look.occasion}
            </p>
            <h3 className="font-serif text-2xl font-medium text-[#141413]">{look.title}</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close flat-lay editor"
            className="p-2 border border-[#E5E3DC] text-[#141413] hover:border-[#141413] transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" strokeWidth={1.5} />
          </button>
        </div>

        {/* Drawer Body */}
        <div className="p-6 space-y-6 flex-1">
          <div className="grid grid-cols-1 sm:grid-cols-12 gap-5 items-start">
            <div className="sm:col-span-8 border border-[#E5E3DC] bg-white p-3">
              <div className="aspect-[3/4] w-full bg-[#F4F3EE] overflow-hidden relative">
                {isEditing ? (
                  <div className="w-full h-full flex flex-col items-center justify-center p-6 text-center">
                    <div className="w-8 h-8 border border-[#141413] border-t-transparent rounded-full animate-spin mb-3" />
                    <p className="font-serif text-lg text-[#141413]">Rendering Edited Flat-Lay</p>
                    <p className="text-xs text-[#68665F] mt-1">
                      Applying instructions with gemini-nano-banana-2.1
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
                  <div className="w-full h-full flex items-center justify-center p-6 text-center text-xs text-[#68665F]">
                    No base flat-lay image yet — enter a prompt below to generate one directly.
                  </div>
                )}
              </div>
              {look.imageDataUrl && !isEditing && (
                <div className="pt-3 flex items-center justify-between text-xs text-[#68665F]">
                  <span className="font-mono">
                    {look.imageHistory.length > 1
                      ? `Iteration ${look.imageHistory.length}`
                      : 'Original Composition'}
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      triggerImageDownload(
                        look.imageDataUrl!,
                        `atelier-${look.occasion.toLowerCase()}-edited.png`
                      )
                    }
                    className="inline-flex items-center gap-1.5 text-[#141413] hover:underline cursor-pointer"
                  >
                    <Download className="w-3.5 h-3.5" strokeWidth={1.5} />
                    <span>Download PNG</span>
                  </button>
                </div>
              )}
            </div>

            {/* Anchor Reference & Version Stack */}
            <div className="sm:col-span-4 space-y-4">
              {sourceGarmentDataUrl && (
                <div className="border border-[#E5E3DC] bg-white p-3 space-y-2">
                  <p className="text-[11px] font-mono uppercase tracking-wider text-[#68665F]">
                    Anchor Garment
                  </p>
                  <div className="aspect-[3/4] w-full bg-[#FAF9F6] border border-[#E5E3DC]/60 overflow-hidden">
                    <img
                      src={sourceGarmentDataUrl}
                      alt="Anchor garment reference"
                      className="w-full h-full object-contain p-1.5"
                      referrerPolicy="no-referrer"
                    />
                  </div>
                </div>
              )}

              {look.imageHistory.length > 1 && (
                <div className="border border-[#E5E3DC] bg-white p-3 space-y-2">
                  <p className="text-[11px] font-mono uppercase tracking-wider text-[#68665F]">
                    Version History
                  </p>
                  <div className="grid grid-cols-3 gap-1.5">
                    {look.imageHistory.map((histUrl, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => handleSelectVersion(histUrl)}
                        className={`aspect-square border overflow-hidden cursor-pointer ${
                          look.imageDataUrl === histUrl
                            ? 'border-[#141413] ring-1 ring-[#141413]'
                            : 'border-[#E5E3DC] opacity-75 hover:opacity-100'
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
              )}
            </div>
          </div>

          {/* Prompt Controls */}
          <div className="border border-[#E5E3DC] bg-white p-5 space-y-4">
            <label htmlFor="flatlay-edit-prompt" className="block text-xs font-mono uppercase tracking-wider text-[#68665F]">
              Describe Your Flat-Lay Refinement
            </label>
            <textarea
              id="flatlay-edit-prompt"
              rows={3}
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="e.g., Swap the loafers for ivory slingback heels, add a structured camel wool blazer, and keep the lighting crisp..."
              className="w-full border border-[#E5E3DC] bg-[#FAF9F6] p-3 text-sm text-[#141413] placeholder:text-[#68665F]/70 focus:outline-none focus:border-[#141413]"
            />

            {/* Quick Suggestion Controls */}
            <div className="space-y-2">
              <p className="text-xs text-[#68665F]">Quick Stylist Adjustments</p>
              <div className="flex flex-wrap gap-2">
                {SUGGESTED_PROMPTS.map((suggestion) => (
                  <button
                    key={suggestion}
                    type="button"
                    onClick={() => setPrompt(suggestion)}
                    className="px-3 py-1.5 text-xs border border-[#E5E3DC] bg-[#FAF9F6] text-[#141413] hover:border-[#141413] transition-colors text-left cursor-pointer"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>

            {/* Aspect Ratio & Resolution */}
            <div className="grid grid-cols-2 gap-4 pt-2 border-t border-[#E5E3DC]">
              <div>
                <p className="text-xs text-[#68665F] mb-1.5">Aspect Ratio</p>
                <div className="flex gap-1.5">
                  {(['3:4', '1:1', '4:3'] as const).map((ratio) => (
                    <button
                      key={ratio}
                      type="button"
                      onClick={() => setAspectRatio(ratio)}
                      className={`px-3 py-1.5 text-xs font-mono border cursor-pointer ${
                        aspectRatio === ratio
                          ? 'bg-[#141413] text-white border-[#141413]'
                          : 'bg-[#FAF9F6] text-[#141413] border-[#E5E3DC]'
                      }`}
                    >
                      {ratio}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <p className="text-xs text-[#68665F] mb-1.5">Output Resolution</p>
                <div className="flex gap-1.5">
                  {(['1K', '2K'] as const).map((sz) => (
                    <button
                      key={sz}
                      type="button"
                      onClick={() => setImageSize(sz)}
                      className={`px-3 py-1.5 text-xs font-mono border cursor-pointer ${
                        imageSize === sz
                          ? 'bg-[#141413] text-white border-[#141413]'
                          : 'bg-[#FAF9F6] text-[#141413] border-[#E5E3DC]'
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
                className="border border-[#9A3412]/30 bg-[#FEF2F2] px-4 py-3 text-xs text-[#7F1D1D]"
              >
                {editError}
              </div>
            )}

            <div className="flex items-center gap-3 pt-2">
              <button
                type="button"
                disabled={!prompt.trim() || isEditing}
                onClick={handleApplyPromptEdit}
                className="flex-1 py-3 px-5 bg-[#141413] text-[#FAF9F6] text-xs font-medium flex items-center justify-center gap-2 hover:bg-[#2B2A28] disabled:bg-[#D8D5CC] disabled:text-[#68665F] disabled:cursor-not-allowed transition-colors cursor-pointer"
              >
                <Wand2 className="w-3.5 h-3.5" strokeWidth={1.5} />
                <span>{isEditing ? 'Applying Edit...' : 'Apply Prompt Edit'}</span>
              </button>

              {look.imageHistory.length > 1 && (
                <button
                  type="button"
                  onClick={() => handleSelectVersion(look.imageHistory[0])}
                  className="py-3 px-4 border border-[#E5E3DC] bg-white text-xs font-medium text-[#141413] hover:border-[#141413] flex items-center gap-1.5 cursor-pointer"
                >
                  <RotateCcw className="w-3.5 h-3.5" strokeWidth={1.5} />
                  <span>Revert Original</span>
                </button>
              )}
            </div>
          </div>

          {look.editPromptHistory.length > 0 && (
            <div className="border border-[#E5E3DC] bg-white p-5 space-y-2">
              <p className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
                Applied Prompts
              </p>
              <ol className="space-y-1.5 text-xs text-[#141413] list-decimal list-inside">
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
