import React, { useRef, useState } from 'react';
import { Upload, Wand2, Download, Bookmark, Trash2, Check } from 'lucide-react';
import { compressImageToDataUrl, splitDataUrl, triggerImageDownload } from '../lib/imageUtils';

interface FlatLayCanvasViewProps {
  onSaveCustomLook: (params: {
    title: string;
    prompt: string;
    imageDataUrl: string;
    referenceImageDataUrl: string | null;
  }) => Promise<void>;
}

const CANVAS_TEMPLATES = [
  'Minimalist Parisian autumn flat-lay: oversized camel cashmere coat, crisp white poplin shirt, raw indigo straight denim, espresso leather loafers, gold watch on warm alabaster paper',
  'Gallery night-out flat-lay: black silk bias-cut slip dress, silver metallic strappy heels, sculptural onyx clutch, crystal drop earrings on pale limestone',
  'Warm-weather linen resort flat-lay: olive relaxed linen blazer, ivory ribbed tank, pleated ecru trousers, woven tan leather sandals, tortoise sunglasses',
];

export function FlatLayCanvasView({ onSaveCustomLook }: FlatLayCanvasViewProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [prompt, setPrompt] = useState('');
  const [title, setTitle] = useState('Custom Flat-Lay Study');
  const [referenceDataUrl, setReferenceDataUrl] = useState<string | null>(null);
  const [generatedUrl, setGeneratedUrl] = useState<string | null>(null);
  const [history, setHistory] = useState<string[]>([]);
  const [promptLog, setPromptLog] = useState<string[]>([]);
  const [aspectRatio, setAspectRatio] = useState<'3:4' | '1:1' | '4:3' | '16:9'>('3:4');
  const [imageSize, setImageSize] = useState<'1K' | '2K'>('1K');
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [savedSuccess, setSavedSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
        title: title.trim() || 'Custom Flat-Lay Study',
        prompt: promptLog.join(' → ') || 'Custom flat-lay composition',
        imageDataUrl: generatedUrl,
        referenceImageDataUrl: referenceDataUrl,
      });
      setSavedSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save to cloud wardrobe.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <section className="max-w-[1280px] mx-auto px-6 md:px-10 py-10">
      <div className="border-b border-[#E5E3DC] pb-6 mb-8 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <p className="text-xs font-mono uppercase tracking-wider text-[#8A6A42]">
            Direct Image Creation &amp; Editing
          </p>
          <h1 className="font-serif text-3xl md:text-4xl font-medium text-[#141413] mt-1">
            Flat-Lay Prompt Canvas
          </h1>
        </div>
        <p className="text-sm text-[#68665F] max-w-[48ch]">
          Compose original flat-lay concepts from text prompts or upload any outfit photo to edit
          pieces, colors, and accessories iteratively using gemini-nano-banana-2.1.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">
        {/* Left: Prompt Composer */}
        <div className="lg:col-span-5 space-y-6">
          <div className="border border-[#E5E3DC] bg-white p-6 space-y-5">
            <div>
              <label htmlFor="custom-look-title" className="block text-xs font-mono uppercase tracking-wider text-[#68665F] mb-2">
                Look Title
              </label>
              <input
                id="custom-look-title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={120}
                className="w-full border border-[#E5E3DC] bg-[#FAF9F6] px-3 py-2 text-sm text-[#141413] focus:outline-none focus:border-[#141413]"
              />
            </div>

            <div>
              <label className="block text-xs font-mono uppercase tracking-wider text-[#68665F] mb-2">
                Optional Reference Photo (For Editing)
              </label>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                onChange={handleUploadReference}
                className="hidden"
              />
              {referenceDataUrl ? (
                <div className="flex items-center justify-between gap-3 p-2.5 border border-[#E5E3DC] bg-[#FAF9F6]">
                  <div className="flex items-center gap-3 min-w-0">
                    <img
                      src={referenceDataUrl}
                      alt="Reference upload"
                      className="w-12 h-12 object-cover border border-[#E5E3DC]"
                      referrerPolicy="no-referrer"
                    />
                    <span className="text-xs text-[#141413] truncate">Reference image attached</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setReferenceDataUrl(null)}
                    aria-label="Remove reference image"
                    className="p-2 text-[#68665F] hover:text-[#141413] cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4" strokeWidth={1.5} />
                  </button>
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="w-full py-3 px-4 border border-dashed border-[#C9C6BC] bg-[#FAF9F6] text-xs text-[#141413] hover:border-[#141413] flex items-center justify-center gap-2 transition-colors cursor-pointer"
                >
                  <Upload className="w-3.5 h-3.5" strokeWidth={1.5} />
                  <span>Upload Base Garment or Flat-Lay Photo</span>
                </button>
              )}
            </div>

            <div>
              <label htmlFor="canvas-prompt" className="block text-xs font-mono uppercase tracking-wider text-[#68665F] mb-2">
                {generatedUrl ? 'Follow-Up Edit or New Prompt' : 'Flat-Lay Text Prompt'}
              </label>
              <textarea
                id="canvas-prompt"
                rows={4}
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder={
                  generatedUrl
                    ? 'e.g., Change the jacket to dark navy wool and add minimalist silver rings...'
                    : 'Describe every garment, color, material, and background surface for your flat-lay...'
                }
                className="w-full border border-[#E5E3DC] bg-[#FAF9F6] p-3 text-sm text-[#141413] placeholder:text-[#68665F]/70 focus:outline-none focus:border-[#141413]"
              />
            </div>

            {/* Aspect Ratio & Resolution */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-[#68665F] mb-1.5">Aspect Ratio</p>
                <div className="flex flex-wrap gap-1.5">
                  {(['3:4', '1:1', '4:3', '16:9'] as const).map((ratio) => (
                    <button
                      key={ratio}
                      type="button"
                      onClick={() => setAspectRatio(ratio)}
                      className={`px-2.5 py-1.5 text-xs font-mono border cursor-pointer ${
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
                <p className="text-xs text-[#68665F] mb-1.5">Resolution</p>
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

            {error && (
              <div
                role="alert"
                className="border border-[#9A3412]/30 bg-[#FEF2F2] px-4 py-3 text-xs text-[#7F1D1D]"
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
                    className="flex-1 py-3 px-4 bg-[#141413] text-[#FAF9F6] text-xs font-medium flex items-center justify-center gap-2 hover:bg-[#2B2A28] disabled:bg-[#D8D5CC] disabled:text-[#68665F] cursor-pointer"
                  >
                    <Wand2 className="w-3.5 h-3.5" strokeWidth={1.5} />
                    <span>{isGenerating ? 'Refining...' : 'Edit Current Flat-Lay'}</span>
                  </button>
                  <button
                    type="button"
                    disabled={!prompt.trim() || isGenerating}
                    onClick={() => handleGenerate('create')}
                    className="py-3 px-4 border border-[#141413] bg-white text-[#141413] text-xs font-medium hover:bg-[#FAF9F6] disabled:opacity-50 cursor-pointer whitespace-nowrap"
                  >
                    Generate Fresh
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  disabled={!prompt.trim() || isGenerating}
                  onClick={() => handleGenerate('create')}
                  className="w-full py-3 px-5 bg-[#141413] text-[#FAF9F6] text-xs font-medium flex items-center justify-center gap-2 hover:bg-[#2B2A28] disabled:bg-[#D8D5CC] disabled:text-[#68665F] cursor-pointer"
                >
                  <Wand2 className="w-3.5 h-3.5" strokeWidth={1.5} />
                  <span>{isGenerating ? 'Creating Flat-Lay...' : 'Create Flat-Lay Image'}</span>
                </button>
              )}
            </div>
          </div>

          {/* Starter Prompt Ideas */}
          <div className="border border-[#E5E3DC] bg-white p-5 space-y-3">
            <p className="text-xs font-mono uppercase tracking-wider text-[#68665F]">
              Editorial Prompt Starters
            </p>
            <div className="space-y-2">
              {CANVAS_TEMPLATES.map((tpl, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setPrompt(tpl)}
                  className="w-full text-left p-3 text-xs text-[#141413] border border-[#E5E3DC] bg-[#FAF9F6] hover:border-[#141413] transition-colors leading-relaxed cursor-pointer"
                >
                  {tpl}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Right: Output Frame */}
        <div className="lg:col-span-7 border border-[#E5E3DC] bg-white p-6 space-y-5">
          <div className="aspect-[3/4] max-h-[680px] w-full bg-[#F4F3EE] border border-[#E5E3DC] overflow-hidden flex items-center justify-center">
            {isGenerating ? (
              <div className="text-center p-8">
                <div className="w-8 h-8 border border-[#141413] border-t-transparent rounded-full animate-spin mx-auto mb-3" />
                <p className="font-serif text-xl text-[#141413]">Composing on Gallery Paper</p>
                <p className="text-xs text-[#68665F] mt-1">Generating with gemini-nano-banana-2.1</p>
              </div>
            ) : generatedUrl ? (
              <img
                src={generatedUrl}
                alt={title}
                className="w-full h-full object-contain"
                referrerPolicy="no-referrer"
              />
            ) : (
              <div className="text-center p-10 max-w-md space-y-2">
                <p className="font-serif text-2xl text-[#141413]">Blank Gallery Paper</p>
                <p className="text-xs text-[#68665F] leading-relaxed">
                  Enter a prompt on the left—or attach a garment photo to remix—and your generated
                  flat-lay photograph will appear here.
                </p>
              </div>
            )}
          </div>

          {generatedUrl && (
            <div className="flex flex-wrap items-center justify-between gap-4 pt-2 border-t border-[#E5E3DC]">
              <div className="flex items-center gap-2">
                {history.length > 1 && (
                  <div className="flex items-center gap-1.5">
                    {history.map((url, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setGeneratedUrl(url)}
                        className={`w-9 h-9 border overflow-hidden cursor-pointer ${
                          generatedUrl === url ? 'border-[#141413]' : 'border-[#E5E3DC] opacity-60'
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
                  className="py-2.5 px-4 border border-[#E5E3DC] bg-white text-xs font-medium text-[#141413] hover:border-[#141413] flex items-center gap-1.5 cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" strokeWidth={1.5} />
                  <span>Download</span>
                </button>

                <button
                  type="button"
                  disabled={isSaving}
                  onClick={handleSave}
                  className="py-2.5 px-4 bg-[#141413] text-[#FAF9F6] text-xs font-medium hover:bg-[#2B2A28] flex items-center gap-1.5 cursor-pointer"
                >
                  {savedSuccess ? (
                    <Check className="w-3.5 h-3.5" strokeWidth={1.5} />
                  ) : (
                    <Bookmark className="w-3.5 h-3.5" strokeWidth={1.5} />
                  )}
                  <span>{isSaving ? 'Saving...' : savedSuccess ? 'Saved to Wardrobe' : 'Save Look'}</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
