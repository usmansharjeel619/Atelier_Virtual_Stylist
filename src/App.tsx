import { useEffect, useState } from 'react';
import { onAuthStateChanged, User } from 'firebase/auth';
import {
  GarmentAnalysis,
  GeneratedLookState,
  SampleGarment,
  SavedGarmentRecord,
  SavedLookRecord,
  StyleBrief,
  StyleInsight,
} from './types/stylist';
import {
  auth,
  ensureUserProfile,
  saveGarmentAndLookToCloud,
  signInWithGoogle,
  signOutUser,
  subscribeToWardrobeGarments,
  testFirestoreConnection,
} from './lib/firebase';
import {
  compressImageToDataUrl,
  splitDataUrl,
  urlToCompressedDataUrl,
} from './lib/imageUtils';
import { ActiveTab, TopBar } from './components/TopBar';
import { GarmentIntake } from './components/GarmentIntake';
import { GarmentDossier } from './components/GarmentDossier';
import { LookTriptych } from './components/LookTriptych';
import { FlatLayEditDrawer } from './components/FlatLayEditDrawer';
import { FlatLayCanvasView } from './components/FlatLayCanvasView';
import { WardrobeView } from './components/WardrobeView';

export default function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('studio');
  const [user, setUser] = useState<User | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [wardrobeGarments, setWardrobeGarments] = useState<SavedGarmentRecord[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Studio state
  const [sourceImageDataUrl, setSourceImageDataUrl] = useState<string | null>(null);
  const [activeGarmentLabel, setActiveGarmentLabel] = useState<string | null>(null);
  const [activeGarmentId, setActiveGarmentId] = useState<string | undefined>(undefined);
  const [brief, setBrief] = useState<StyleBrief>({
    season: 'Year-Round',
    silhouette: 'Tailored',
    budgetTier: 'Contemporary',
  });

  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analyzeError, setAnalyzeError] = useState<string | null>(null);
  const [analysis, setAnalysis] = useState<GarmentAnalysis | null>(null);
  const [isLoadingInsight, setIsLoadingInsight] = useState(false);
  const [insightError, setInsightError] = useState<string | null>(null);
  const [isLoadingQuickTips, setIsLoadingQuickTips] = useState(false);
  const [quickTipsError, setQuickTipsError] = useState<string | null>(null);
  const [looks, setLooks] = useState<GeneratedLookState[]>([]);
  const [savingLookId, setSavingLookId] = useState<string | null>(null);
  const [refiningLookId, setRefiningLookId] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    window.setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 4500);
  };

  useEffect(() => {
    testFirestoreConnection();
    const unsub = onAuthStateChanged(auth, (nextUser) => {
      setUser(nextUser);
      setAuthReady(true);
      if (nextUser) {
        ensureUserProfile(nextUser).catch((err) => {
          console.error(err);
        });
      }
    });
    return () => unsub();
  }, []);

  useEffect(() => {
    if (!authReady || !user) {
      setWardrobeGarments([]);
      return;
    }

    const unsub = subscribeToWardrobeGarments(
      user.uid,
      (items) => setWardrobeGarments(items),
      (err) => {
        showToast(err.message);
      }
    );
    return () => unsub();
  }, [authReady, user]);

  const handleSignIn = async () => {
    try {
      await signInWithGoogle();
      showToast('Signed in to your Atelier Cloud Wardrobe.');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Google Sign-In was cancelled or failed.');
    }
  };

  const handleSignOut = async () => {
    await signOutUser();
    showToast('Signed out.');
  };

  const handleUploadFile = async (file: File) => {
    try {
      setAnalyzeError(null);
      const parsed = await compressImageToDataUrl(file, 1024, 0.86, 650000);
      setSourceImageDataUrl(parsed.dataUrl);
      setActiveGarmentLabel(file.name.replace(/\.[^.]+$/, ''));
      setActiveGarmentId(undefined);
      setAnalysis(null);
      setLooks([]);
    } catch (err) {
      setAnalyzeError(err instanceof Error ? err.message : 'Unable to process image file.');
    }
  };

  const handleSelectSample = async (sample: SampleGarment) => {
    try {
      setAnalyzeError(null);
      const parsed = await urlToCompressedDataUrl(sample.imagePath, 1024, 0.86);
      setSourceImageDataUrl(parsed.dataUrl);
      setActiveGarmentLabel(sample.title);
      setActiveGarmentId(undefined);
      setBrief((prev) => ({ ...prev, season: sample.defaultSeason }));
      setAnalysis(null);
      setLooks([]);
    } catch (err) {
      setAnalyzeError(err instanceof Error ? err.message : 'Unable to load sample garment.');
    }
  };

  const handleClearPiece = () => {
    setSourceImageDataUrl(null);
    setActiveGarmentLabel(null);
    setActiveGarmentId(undefined);
    setAnalysis(null);
    setLooks([]);
    setAnalyzeError(null);
  };

  const generateSingleFlatLay = async (
    lookState: GeneratedLookState,
    garmentDataUrl: string,
    garmentTitle: string,
    currentBrief: StyleBrief
  ) => {
    setLooks((prev) =>
      prev.map((l) =>
        l.id === lookState.id ? { ...l, isGenerating: true, error: null } : l
      )
    );

    try {
      const { base64, mimeType } = splitDataUrl(garmentDataUrl);
      const res = await fetch('/api/stylist/generate-flatlay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          garmentImageBase64: base64,
          garmentMimeType: mimeType,
          occasion: lookState.occasion,
          garmentTitle,
          lookTitle: lookState.title,
          flatLayDirection: lookState.flatLayDirection,
          pieces: lookState.pieces,
          season: currentBrief.season,
          silhouette: currentBrief.silhouette,
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        setLooks((prev) =>
          prev.map((l) =>
            l.id === lookState.id
              ? {
                  ...l,
                  isGenerating: false,
                  error: data.error || 'Could not generate flat-lay image.',
                  requiresPaidKey: Boolean(data.requiresPaidKey),
                }
              : l
          )
        );
        return;
      }

      setLooks((prev) =>
        prev.map((l) =>
          l.id === lookState.id
            ? {
                ...l,
                isGenerating: false,
                imageDataUrl: data.imageDataUrl,
                imageHistory: [data.imageDataUrl],
                error: null,
              }
            : l
        )
      );
    } catch (err) {
      setLooks((prev) =>
        prev.map((l) =>
          l.id === lookState.id
            ? {
                ...l,
                isGenerating: false,
                error: err instanceof Error ? err.message : 'Network error generating flat-lay.',
              }
            : l
        )
      );
    }
  };

  const handleAnalyzeAndStyle = async () => {
    if (!sourceImageDataUrl) return;

    setIsAnalyzing(true);
    setAnalyzeError(null);
    setAnalysis(null);
    setLooks([]);

    try {
      const { base64, mimeType } = splitDataUrl(sourceImageDataUrl);
      const res = await fetch('/api/stylist/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageBase64: base64,
          mimeType,
          season: brief.season,
          silhouette: brief.silhouette,
          budgetTier: brief.budgetTier,
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Failed to analyze garment.');
      }

      const parsedAnalysis = data as GarmentAnalysis;
      const newGarmentId = `garment_${Date.now()}`;
      setAnalysis(parsedAnalysis);
      setActiveGarmentId(newGarmentId);
      setActiveGarmentLabel(parsedAnalysis.garmentTitle);

      const initialLooks: GeneratedLookState[] = (parsedAnalysis.outfits || []).map(
        (outfit, idx) => ({
          id: `look_${idx}_${Date.now()}`,
          occasion: outfit.occasion,
          title: outfit.title,
          stylingRationale: outfit.stylingRationale,
          flatLayDirection: outfit.flatLayDirection,
          pieces: outfit.pieces,
          imageDataUrl: null,
          imageHistory: [],
          editPromptHistory: [],
          isGenerating: true,
          error: null,
        })
      );

      setLooks(initialLooks);

      // Fire all 3 flat-lay renders concurrently so each settles independently
      await Promise.all(
        initialLooks.map((lookItem) =>
          generateSingleFlatLay(
            lookItem,
            sourceImageDataUrl,
            parsedAnalysis.garmentTitle,
            brief
          )
        )
      );
    } catch (err) {
      setAnalyzeError(err instanceof Error ? err.message : 'Failed to analyze garment.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleRetryFlatLay = (lookId: string) => {
    const target = looks.find((l) => l.id === lookId);
    if (!target || !sourceImageDataUrl || !analysis) return;
    generateSingleFlatLay(target, sourceImageDataUrl, analysis.garmentTitle, brief);
  };

  const handleRefreshInsight = async () => {
    if (!analysis) return;
    setIsLoadingInsight(true);
    setInsightError(null);

    try {
      const payload: Record<string, unknown> = {
        garmentTitle: analysis.garmentTitle,
        category: analysis.category,
        fabricNotes: analysis.fabricNotes,
        season: brief.season,
        silhouette: brief.silhouette,
      };

      if (sourceImageDataUrl) {
        const { base64, mimeType } = splitDataUrl(sourceImageDataUrl);
        payload.imageBase64 = base64;
        payload.mimeType = mimeType;
      }

      const res = await fetch('/api/stylist/style-insight', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Failed to generate Style Insight.');
      }

      const nextInsight = data as StyleInsight;
      setAnalysis((prev) => (prev ? { ...prev, styleInsight: nextInsight } : prev));
    } catch (err) {
      setInsightError(
        err instanceof Error ? err.message : 'Unable to refresh Style Insight.'
      );
    } finally {
      setIsLoadingInsight(false);
    }
  };

  const handleRefreshQuickTips = async () => {
    if (!analysis) return;
    setIsLoadingQuickTips(true);
    setQuickTipsError(null);

    try {
      const payload: Record<string, unknown> = {
        garmentTitle: analysis.garmentTitle,
        category: analysis.category,
        fabricNotes: analysis.fabricNotes,
        season: brief.season,
        silhouette: brief.silhouette,
      };

      if (sourceImageDataUrl) {
        const { base64, mimeType } = splitDataUrl(sourceImageDataUrl);
        payload.imageBase64 = base64;
        payload.mimeType = mimeType;
      }

      const res = await fetch('/api/stylist/quick-tips', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || 'Failed to generate Quick Tips.');
      }

      const nextTips = Array.isArray(data.quickTips) ? (data.quickTips as string[]) : [];
      setAnalysis((prev) => (prev ? { ...prev, quickTips: nextTips } : prev));
    } catch (err) {
      setQuickTipsError(
        err instanceof Error ? err.message : 'Unable to refresh Quick Tips.'
      );
    } finally {
      setIsLoadingQuickTips(false);
    }
  };

  const handleSaveLook = async (look: GeneratedLookState) => {
    let currentUser = user;
    if (!currentUser) {
      try {
        currentUser = await signInWithGoogle();
      } catch {
        showToast('Sign in with Google to save outfits to your Cloud Wardrobe.');
        return;
      }
    }

    if (!analysis || !sourceImageDataUrl) return;
    setSavingLookId(look.id);
    try {
      const { garmentId, lookId } = await saveGarmentAndLookToCloud({
        user: currentUser,
        garmentId: activeGarmentId,
        analysis,
        brief,
        sourceImageDataUrl,
        look,
      });
      setActiveGarmentId(garmentId);
      setLooks((prev) =>
        prev.map((l) => (l.id === look.id ? { ...l, savedLookId: lookId } : l))
      );
      showToast(`Saved "${look.title}" to your Cloud Wardrobe.`);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to save look.');
    } finally {
      setSavingLookId(null);
    }
  };

  const handleSaveCustomLook = async (params: {
    title: string;
    prompt: string;
    imageDataUrl: string;
    referenceImageDataUrl: string | null;
  }) => {
    let currentUser = user;
    if (!currentUser) {
      currentUser = await signInWithGoogle();
    }

    const customAnalysis: GarmentAnalysis = {
      garmentTitle: params.title,
      category: 'Custom Flat-Lay',
      fabricNotes: 'Prompt-composed flat-lay study.',
      styleSummary: params.prompt,
      palette: [{ hex: '#141413', name: 'Editorial Ink', role: 'Dominant' }],
      outfits: [],
    };

    const customLook: GeneratedLookState = {
      id: `custom_${Date.now()}`,
      occasion: 'Custom',
      title: params.title,
      stylingRationale: params.prompt,
      flatLayDirection: params.prompt,
      pieces: [
        {
          slot: 'Composition',
          name: params.title,
          color: 'Curated',
          material: 'Flat-Lay Canvas',
        },
      ],
      imageDataUrl: params.imageDataUrl,
      imageHistory: [params.imageDataUrl],
      editPromptHistory: [params.prompt],
      isGenerating: false,
      error: null,
    };

    await saveGarmentAndLookToCloud({
      user: currentUser,
      analysis: customAnalysis,
      brief,
      sourceImageDataUrl: params.referenceImageDataUrl || params.imageDataUrl,
      look: customLook,
    });
    showToast(`Saved "${params.title}" to your Cloud Wardrobe.`);
  };

  const handleReopenGarmentInStudio = (
    garment: SavedGarmentRecord,
    savedLooks: SavedLookRecord[]
  ) => {
    setSourceImageDataUrl(garment.sourceImageDataUrl);
    setActiveGarmentLabel(garment.title);
    setActiveGarmentId(garment.id);
    setBrief({
      season: garment.season,
      silhouette: garment.silhouette,
      budgetTier: garment.budgetTier,
    });
    setAnalysis({
      garmentTitle: garment.title,
      category: garment.category,
      fabricNotes: garment.fabricNotes,
      styleSummary: garment.styleSummary,
      palette: garment.palette,
      styleInsight: garment.styleInsight,
      quickTips: garment.quickTips,
      outfits: [],
    });
    setLooks(
      savedLooks.map((sl) => ({
        id: sl.id,
        savedLookId: sl.id,
        occasion: sl.occasion,
        title: sl.title,
        stylingRationale: sl.stylingRationale,
        flatLayDirection: sl.stylingRationale,
        pieces: sl.pieces,
        imageDataUrl: sl.flatLayImageDataUrl || null,
        imageHistory: sl.flatLayImageDataUrl ? [sl.flatLayImageDataUrl] : [],
        editPromptHistory: sl.editPromptHistory || [],
        isGenerating: false,
        error: null,
      }))
    );
    setActiveTab('studio');
  };

  const refiningLook = looks.find((l) => l.id === refiningLookId) ?? null;

  return (
    <div className="min-h-screen flex flex-col bg-[#FAF9F6] text-[#141413]">
      <TopBar
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        user={user}
        authReady={authReady}
        wardrobeCount={wardrobeGarments.length}
        onSignIn={handleSignIn}
        onSignOut={handleSignOut}
      />

      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-40 max-w-sm border border-[#141413] bg-white px-4 py-3 text-xs text-[#141413] shadow-sm">
          {toastMessage}
        </div>
      )}

      <main className="flex-1">
        {activeTab === 'studio' && (
          <>
            <GarmentIntake
              sourceImageDataUrl={sourceImageDataUrl}
              activeGarmentLabel={activeGarmentLabel}
              brief={brief}
              isAnalyzing={isAnalyzing}
              analyzeError={analyzeError}
              onChangeBrief={setBrief}
              onUploadFile={handleUploadFile}
              onSelectSample={handleSelectSample}
              onClearPiece={handleClearPiece}
              onAnalyzeAndStyle={handleAnalyzeAndStyle}
            />

            {analysis && (
              <>
                <GarmentDossier
                  analysis={analysis}
                  brief={brief}
                  isLoadingInsight={isLoadingInsight}
                  insightError={insightError}
                  onRefreshInsight={handleRefreshInsight}
                  isLoadingQuickTips={isLoadingQuickTips}
                  quickTipsError={quickTipsError}
                  onRefreshQuickTips={handleRefreshQuickTips}
                />
                <LookTriptych
                  looks={looks}
                  garmentTitle={analysis.garmentTitle}
                  savingLookId={savingLookId}
                  onRetryFlatLay={handleRetryFlatLay}
                  onOpenRefine={(look) => setRefiningLookId(look.id)}
                  onSaveLook={handleSaveLook}
                />
              </>
            )}
          </>
        )}

        {activeTab === 'canvas' && (
          <FlatLayCanvasView onSaveCustomLook={handleSaveCustomLook} />
        )}

        {activeTab === 'wardrobe' && (
          <WardrobeView
            user={user}
            garments={wardrobeGarments}
            onSignIn={handleSignIn}
            onReopenGarmentInStudio={handleReopenGarmentInStudio}
          />
        )}
      </main>

      <footer className="border-t border-[#E5E3DC] bg-white">
        <div className="max-w-[1280px] mx-auto px-6 md:px-10 py-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-[#68665F]">
          <span className="font-serif text-base text-[#141413]">Atelier Virtual Stylist</span>
          <span>Editorial Wardrobe Analysis · Flat-Lay Studio · Cloud Sync</span>
        </div>
      </footer>

      <FlatLayEditDrawer
        look={refiningLook}
        sourceGarmentDataUrl={sourceImageDataUrl}
        onClose={() => setRefiningLookId(null)}
        onUpdateLook={(updated) => {
          setLooks((prev) => prev.map((l) => (l.id === updated.id ? updated : l)));
        }}
      />
    </div>
  );
}
