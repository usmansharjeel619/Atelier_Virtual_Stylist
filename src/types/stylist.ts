export type SeasonOption = 'Spring/Summer' | 'Autumn/Winter' | 'Year-Round';
export type SilhouetteOption = 'Tailored' | 'Relaxed' | 'Sculptural';
export type BudgetTierOption = 'Accessible' | 'Contemporary' | 'Designer';
export type OccasionType = 'Casual' | 'Business' | 'Night Out' | 'Custom';

export interface StyleBrief {
  season: SeasonOption;
  silhouette: SilhouetteOption;
  budgetTier: BudgetTierOption;
}

export interface PaletteSwatch {
  hex: string;
  name: string;
  role: string;
}

export interface OutfitPiece {
  slot: string;
  name: string;
  color: string;
  material: string;
}

export interface OutfitBlueprint {
  occasion: 'Casual' | 'Business' | 'Night Out';
  title: string;
  stylingRationale: string;
  flatLayDirection: string;
  pieces: OutfitPiece[];
}

export interface StyleInsight {
  fitAndProportion: string;
  materialAndDrape: string;
  howToWear: string;
  whatToAvoid: string;
}

export interface GarmentAnalysis {
  garmentTitle: string;
  category: string;
  fabricNotes: string;
  styleSummary: string;
  palette: PaletteSwatch[];
  styleInsight?: StyleInsight;
  quickTips?: string[];
  outfits: OutfitBlueprint[];
}

export interface GeneratedLookState {
  id: string;
  occasion: OccasionType;
  title: string;
  stylingRationale: string;
  flatLayDirection: string;
  pieces: OutfitPiece[];
  imageDataUrl: string | null;
  imageHistory: string[];
  editPromptHistory: string[];
  isGenerating: boolean;
  error: string | null;
  requiresPaidKey?: boolean;
  savedLookId?: string;
}

export interface SampleGarment {
  id: string;
  title: string;
  subtitle: string;
  imagePath: string;
  defaultSeason: SeasonOption;
}

export interface SavedGarmentRecord {
  id: string;
  ownerId: string;
  title: string;
  category: string;
  fabricNotes: string;
  styleSummary: string;
  season: SeasonOption;
  silhouette: SilhouetteOption;
  budgetTier: BudgetTierOption;
  palette: PaletteSwatch[];
  styleInsight?: StyleInsight;
  quickTips?: string[];
  sourceImageDataUrl: string;
  createdAt?: unknown;
  updatedAt?: unknown;
}

export interface SavedLookRecord {
  id: string;
  ownerId: string;
  garmentId: string;
  occasion: OccasionType;
  title: string;
  stylingRationale: string;
  pieces: OutfitPiece[];
  flatLayImageDataUrl?: string;
  editPromptHistory: string[];
  createdAt?: unknown;
  updatedAt?: unknown;
}
