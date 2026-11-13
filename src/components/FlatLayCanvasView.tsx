import React, { useEffect, useRef, useState } from 'react';
import { Upload, Zap, Download, Bookmark, Trash2, Check } from 'lucide-react';
import { compressImageToDataUrl, splitDataUrl, triggerImageDownload } from '../lib/imageUtils';

export interface EditorSeed {
  id: string;
  title: string;
  referenceDataUrl: string | null;
  generatedUrl: string | null;
  history: string[];
  prompts: string[];
}

interface FlatLayCanvasViewProps {
  seed?: EditorSeed | null;
  onSaveCustomLook: (params: {
    title: string;
    prompt: string;
    imageDataUrl: string;
    referenceImageDataUrl: string | null;
  }) => Promise<void>;
}

const QUICK_MODS = [
  'Swap shoes for chunky silver sneakers',
  'Add oversized boxy leather moto jacket',
  'Change background to brushed steel & concrete',
  'Add oval tinted sunglasses + chrome shoulder bag',
];

const CANVAS_TEMPLATES = [
  'Y2K Berlin club flat-lay: cropped moto jacket, washed baggy cargo denim, chunky silver runners, chrome oval shades on raw concrete',
  'Quiet-chaos Copenhagen street fit: boxy lime knit, pleated charcoal wool skirt, espresso square-toe boots, nylon sling bag on chalk paper',
  'Gallery night-out flat-lay: black silk bias-cut slip dress, silver metallic strappy heels, sculptural onyx clutch on pale limestone',
];

export function FlatLayCanvasView({ seed, onSaveCustomLook }: FlatLayCanvasViewProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [prompt, setPrompt] = useState('');
  const [title, setTitle] = useState('Custom Fit Remix');
  const [referenceDataUrl, setReferenceDataUrl] = useState<string | null>(null);
  const [generatedUrl, setGeneratedUrl] = useState<string | null>(null);
  const [history, setHistory] = useState<string[]>([]);
  const [promptLog, setPromptLog] = useState<string[]>([]);
  const [showOriginal, setShowOriginal] = useState(false);
  const [aspectRatio, setAspectRatio] = useState<'3:4' | '1:1' | '4:3' | '9:16'>('3:4');
  const [imageSize, setImageSize] = useState<'1K' | '2K'>('1K');
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [savedSuccess, setSavedSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!seed) return;
    setTitle(seed.title || 'Custom Fit Remix');
    setReferenceDataUrl(seed.referenceDataUrl);
    setGeneratedUrl(seed.generatedUrl);
    setHistory(seed.history);
    setPromptLog(seed.prompts);
    setShowOriginal(false);
    setSavedSuccess(false);
    setError(null);
  }, [seed]);

  const handleUploadReference = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const parsed = await compressImageToDataUrl(file, 1024, 0.85, 650000);
      setReferenceDataUrl(parsed.dataUrl);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not read reference image.');
    }
    e.target.value = '';
  };

  const handleGenerate = async (mode: 'create' | 'edit') => {
    const trimmed = prompt.trim();
    if (!trimmed) return;

    setIsGenerating(true);
    setError(null);
    setSavedSuccess(false);
    setShowOriginal(false);

    try {
      const payload: Record<string, unknown> = {
        editPrompt: trimmed,
        aspectRatio,
        imageSize,
      };

      if (mode === 'edit' && generatedUrl) {
        const base = splitDataUrl(generatedUrl);
        payload.baseImageBase64 = base.base64;
        payload.baseMimeType = base.mimeType;
        if (referenceDataUrl) {
          const anchor = splitDataUrl(referenceDataUrl);
          payload.sourceGarmentBase64 = anchor.base64;
          payload.sourceGarmentMimeType = anchor.mimeType;
        }
      } else if (referenceDataUrl) {
        const ref = splitDataUrl(referenceDataUrl);
        payload.baseImageBase64 = ref.base64;
        payload.baseMimeType = ref.mimeType;
      }

      const res = await fetch('/api/stylist/edit-flatlay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Failed to generate image.');
      }

      setGeneratedUrl(data.imageDataUrl);
      setHistory((prev) => [...prev, data.imageDataUrl]);
      setPromptLog((prev) => [...prev, trimmed]);
      setPrompt('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to generate image.');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSave = async () => {
    if (!generatedUrl) return;
    setIsSaving(true);
    setError(null);
    try {
      await onSaveCustomLook({
        title: title.trim() || 'Custom Fit Remix',
        prompt: promptLog.join(' → ') || 'Custom flat-lay remix',
        imageDataUrl: generatedUrl,
        referenceImageDataUrl: referenceDataUrl,
      });
      setSavedSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save to cloud closet.');
    } finally {
      setIsSaving(false);
    }
  };

  const displayedImage = showOriginal && history.length > 0 ? history[0] : generatedUrl;

  return (
    <section className="max-w-[1280px] mx-auto px-4 sm:px-6 md:px-10 py-10">
      <div className="border-b-[3px] border-[#111111] pb-6 mb-9 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-2">
          <span className="inline-block -rotate-2 bg-[#D5A8FF] text-[#111111] zine-sticker px-3 py-0.5 text-xs font-mono font-bold uppercase tracking-wider">
            Interactive Flat-Lay Editor
          </span>
          <h1 className="font-display text-3xl md:text-5xl font-extrabold text-[#111111]">
            Remix &amp; Prompt Studio
          </h1>
        </div>
        <div className="flex items-center gap-3">
          {history.length > 1 && (
            <button
              type="button"
              onClick={() => setShowOriginal((prev) => !prev)}
              className={`zine-btn px-3.5 py-2 text-xs font-mono font-bold uppercase cursor-pointer ${
                showOriginal ? 'bg-[#FF3D6E] text-white' : 'bg-[#D4FF38] text-[#111111]'
              }`}
            >
              {showOriginal ? 'Viewing Original v1' : 'Compare v1'}
            </button>
          )}
          <p className="text-xs font-mono font-bold text-[#545149] max-w-[38ch] hidden sm:block">
            Build flat-lays from text or remix any Studio drop with instant prompt swaps.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-10 items-start">
        {/* Left: Prompt Composer */}
        <div className="lg:col-span-5 space-y-7">
          <div className="relative pt-3">
            <div className="absolute -top-0.5 left-4 z-10 -rotate-1 bg-[#2B4BFF] text-white zine-sticker px-3 py-0.5 text-[11px] font-mono font-bold uppercase tracking-wider">
              Remix Controls
            </div>

            <div className="zine-card-cobalt p-5 sm:p-6 pt-7 space-y-5">
              <div>
                <label
                  htmlFor="custom-look-title"
                  className="block text-xs font-mono font-bold uppercase tracking-wider text-[#2B4BFF] mb-2"
                >
                  Fit Title
                </label>
                <input
                  id="custom-look-title"
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={120}
                  className="w-full border-[3px] border-[#111111] bg-[#F6F4EC] px-3 py-2 text-sm font-bold text-[#111111] focus:outline-none focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-mono font-bold uppercase tracking-wider text-[#545149] mb-2">
                  Garment Reference (Optional)
                </label>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  onChange={handleUploadReference}
                  className="hidden"
                />
                {referenceDataUrl ? (
                  <div className="flex items-center justify-between gap-3 p-2.5 border-[3px] border-[#111111] shadow-[4px_4px_0_#111111] bg-[#D4FF38]">
                    <div className="flex items-center gap-3 min-w-0">
                      <img
                        src={referenceDataUrl}
                        alt="Reference upload"
                        className="w-12 h-12 -rotate-2 object-cover border-2 border-[#111111] bg-white"
                        referrerPolicy="no-referrer"
                      />
                      <span className="text-xs font-mono font-bold uppercase text-[#111111] truncate">
                        Anchor Piece Locked
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setReferenceDataUrl(null)}
                      aria-label="Remove reference image"
                      className="p-2 text-[#111111] hover:text-[#FF3D6E] cursor-pointer"
                    >
                      <Trash2 className="w-4 h-4" strokeWidth={2.2} />
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="w-full py-3 px-4 border-[3px] border-dashed border-[#111111] bg-[#F6F4EC] text-xs font-mono font-bold uppercase text-[#111111] hover:bg-[#D5A8FF]/35 flex items-center justify-center gap-2 transition-colors cursor-pointer"
                  >
                    <Upload className="w-4 h-4" strokeWidth={2.2} />
                    <span>Attach Garment or Base Photo</span>
                  </button>
                )}
              </div>

              <div className="space-y-2">
                <p className="text-xs font-mono font-bold uppercase tracking-wider text-[#2B4BFF]">
                  One-Tap Remix Moves
                </p>
                <div className="flex flex-wrap gap-2">
                  {QUICK_MODS.map((mod) => (
                    <button
                      key={mod}
                      type="button"
                      onClick={() => setPrompt(mod)}
                      className={`px-2.5 py-1.5 text-xs font-bold border-2 border-[#111111] text-left cursor-pointer transition-all ${
                        prompt === mod
                          ? '-translate-y-0.5 bg-[#D4FF38] text-[#111111] shadow-[3px_3px_0_#111111]'
                          : 'bg-[#F6F4EC] text-[#111111] hover:bg-[#EBE7DA]'
                      }`}
                    >
                      {mod}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label
                  htmlFor="canvas-prompt"
                  className="block text-xs font-mono font-bold uppercase tracking-wider text-[#545149] mb-2"
                >
                  {generatedUrl ? 'What Should We Swap or Restyle?' : 'Flat-Lay Text Prompt'}
                </label>
                <textarea
                  id="canvas-prompt"
                  rows={4}
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  placeholder={
                    generatedUrl
                      ? 'e.g., Swap the shoes for chunky silver sneakers and add a boxy moto jacket...'
                      : 'Describe pieces, colors, fabrics, and background surface...'
                  }
                  className="w-full border-[3px] border-[#111111] bg-[#F6F4EC] p-3 text-sm font-medium text-[#111111] placeholder:text-[#545149]/70 focus:outline-none focus:bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
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

              {error && (
                <div
                  role="alert"
                  className="border-[3px] border-[#111111] bg-[#FFE5EC] shadow-[4px_4px_0_#FF3D6E] px-4 py-3 text-xs font-bold text-[#9F1239]"
                >
                  {error}
                </div>
              )}

              <div className="flex flex-col sm:flex-row gap-2.5">
                {generatedUrl ? (
                  <>
                    <button
                      type="button"
                      disabled={!prompt.trim() || isGenerating}
                      onClick={() => handleGenerate('edit')}
                      className="zine-btn flex-1 py-3 px-4 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase flex items-center justify-center gap-2 disabled:bg-[#EBE7DA] disabled:text-[#545149] disabled:shadow-none cursor-pointer"
                    >
                      <Zap className="w-3.5 h-3.5 fill-current" strokeWidth={2.5} />
                      <span>{isGenerating ? 'Remixing...' : 'Apply Edit'}</span>
                    </button>
                    <button
                      type="button"
                      disabled={!prompt.trim() || isGenerating}
                      onClick={() => handleGenerate('create')}
                      className="zine-btn py-3 px-4 bg-white text-[#111111] text-xs font-mono font-bold uppercase hover:bg-[#D5A8FF] disabled:opacity-50 cursor-pointer whitespace-nowrap"
                    >
                      Fresh Shot
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    disabled={!prompt.trim() || isGenerating}
                    onClick={() => handleGenerate('create')}
                    className="zine-btn w-full py-3.5 px-5 bg-[#D4FF38] text-[#111111] text-xs font-mono font-bold uppercase flex items-center justify-center gap-2 disabled:bg-[#EBE7DA] disabled:text-[#545149] disabled:shadow-none cursor-pointer"
                  >
                    <Zap className="w-3.5 h-3.5 fill-current" strokeWidth={2.5} />
                    <span>{isGenerating ? 'Rendering...' : 'Generate Flat-Lay'}</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          <div className="relative pt-3">
            <div className="absolute -top-0.5 right-4 z-10 rotate-1 bg-[#FF6B2C] text-white zine-sticker px-3 py-0.5 text-[11px] font-mono font-bold uppercase tracking-wider">
              Gen Z Prompt Templates
            </div>

            <div className="zine-card p-5 pt-7 space-y-3">
              {CANVAS_TEMPLATES.map((tpl, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setPrompt(tpl)}
                  className={`${idx % 2 === 0 ? '-rotate-1' : 'rotate-1'} hover:rotate-0 w-full text-left p-3 text-xs font-medium text-[#111111] border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] bg-[#F6F4EC] hover:bg-[#D4FF38] transition-all leading-relaxed cursor-pointer`}
                >
                  {tpl}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Right: Output Frame */}
        <div className="lg:col-span-7 relative pt-3">
          <div className="absolute -top-0.5 right-6 z-10 rotate-2 bg-[#D4FF38] text-[#111111] zine-sticker px-3 py-1 text-xs font-mono font-bold uppercase">
            {showOriginal && history.length > 0
              ? 'Original v1'
              : generatedUrl
                ? `Canvas · v${Math.max(1, history.length)}`
                : 'Live Canvas'}
          </div>

          <div className="zine-card-lg p-5 sm:p-6 pt-7 space-y-5">
            <div className="aspect-[3/4] max-h-[680px] w-full bg-[#EBE7DA] border-[3px] border-[#111111] overflow-hidden flex items-center justify-center">
              {isGenerating ? (
                <div className="text-center p-8">
                  <div className="w-10 h-10 border-[3px] border-[#111111] border-t-[#D4FF38] rounded-full animate-spin mx-auto mb-3" />
                  <p className="font-display text-xl font-extrabold text-[#111111]">
                    Rendering Your Remix...
                  </p>
                </div>
              ) : displayedImage ? (
                <img
                  src={displayedImage}
                  alt={title}
                  className="w-full h-full object-contain"
                  referrerPolicy="no-referrer"
                />
              ) : (
                <div className="text-center p-10 max-w-md space-y-3">
                  <span className="-rotate-2 inline-block bg-[#D4FF38] border-[3px] border-[#111111] shadow-[3px_3px_0_#111111] px-3 py-1 text-xs font-mono font-bold uppercase">
                    Blank Canvas
                  </span>
                  <p className="font-display text-2xl font-extrabold text-[#111111]">
                    Create or Remix Any Flat-Lay
                  </p>
                  <p className="text-xs font-medium text-[#545149] leading-relaxed">
                    Type a prompt on the left, attach a garment photo, or click Open in Editor on
                    any Studio look to iterate on accessories and silhouettes.
                  </p>
                </div>
              )}
            </div>

            {generatedUrl && (
              <div className="flex flex-wrap items-center justify-between gap-4 pt-3 border-t-[3px] border-[#111111]/15">
                <div className="flex items-center gap-2">
                  {history.length > 1 && (
                    <div className="flex items-center gap-2">
                      {history.map((url, idx) => (
                        <button
                          key={idx}
                          type="button"
                          onClick={() => {
                            setShowOriginal(false);
                            setGeneratedUrl(url);
                          }}
                          className={`w-11 h-11 border-[3px] overflow-hidden cursor-pointer transition-transform ${
                            generatedUrl === url
                              ? 'border-[#2B4BFF] ring-2 ring-[#D4FF38] -rotate-3 scale-105'
                              : 'border-[#111111] opacity-70 hover:opacity-100'
                          }`}
                          title={`Version ${idx + 1}`}
                        >
                          <img
                            src={url}
                            alt={`Version ${idx + 1}`}
                            className="w-full h-full object-cover"
                            referrerPolicy="no-referrer"
                          />
                        </button>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-2.5">
                  <button
                    type="button"
                    onClick={() =>
                      triggerImageDownload(
                        generatedUrl,
                        `${title.toLowerCase().replace(/[^a-z0-9]+/g, '-')}.png`
                      )
                    }
                    className="zine-btn py-2.5 px-4 bg-white text-xs font-mono font-bold uppercase text-[#111111] hover:bg-[#EBE7DA] flex items-center gap-1.5 cursor-pointer"
                  >
                    <Download className="w-3.5 h-3.5" strokeWidth={2.2} />
                    <span>Export</span>
                  </button>

                  <button
                    type="button"
                    disabled={isSaving}
                    onClick={handleSave}
                    className="zine-btn py-2.5 px-4 bg-[#FF3D6E] text-white text-xs font-mono font-bold uppercase flex items-center gap-1.5 cursor-pointer"
                  >
                    {savedSuccess ? (
                      <Check className="w-3.5 h-3.5" strokeWidth={2.5} />
                    ) : (
                      <Bookmark className="w-3.5 h-3.5" strokeWidth={2.2} />
                    )}
                    <span>{isSaving ? 'Saving...' : savedSuccess ? 'Saved' : 'Save Closet'}</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
