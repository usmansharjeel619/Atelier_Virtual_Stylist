import { initializeApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  User,
} from 'firebase/auth';
import {
  getFirestore,
  doc,
  getDocFromServer,
  setDoc,
  deleteDoc,
  collection,
  query,
  orderBy,
  onSnapshot,
  serverTimestamp,
  Unsubscribe,
} from 'firebase/firestore';
import firebaseConfig from '../../firebase-applet-config.json';
import {
  GarmentAnalysis,
  GeneratedLookState,
  SavedGarmentRecord,
  SavedLookRecord,
  StyleBrief,
} from '../types/stylist';
import { compressImageToDataUrl } from './imageUtils';

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app, firebaseConfig.firestoreDatabaseId);
export const auth = getAuth(app);

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId?: string | null;
    email?: string | null;
    emailVerified?: boolean | null;
    isAnonymous?: boolean | null;
    tenantId?: string | null;
    providerInfo?: {
      providerId?: string | null;
      email?: string | null;
    }[];
  };
}

export function handleFirestoreError(
  error: unknown,
  operationType: OperationType,
  path: string | null
): never {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo:
        auth.currentUser?.providerData?.map((provider) => ({
          providerId: provider.providerId,
          email: provider.email,
        })) || [],
    },
    operationType,
    path,
  };
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

export async function testFirestoreConnection(): Promise<void> {
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
  } catch (error) {
    if (error instanceof Error && error.message.includes('the client is offline')) {
      console.error('Please check your Firebase configuration.');
    }
  }
}

export function sanitizeId(raw: string): string {
  const cleaned = raw.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 100);
  return cleaned || `item_${Date.now()}`;
}

function clampStr(val: string | undefined, maxLen: number, fallback = 'Untitled'): string {
  const trimmed = (val ?? '').trim();
  if (!trimmed) return fallback;
  return trimmed.slice(0, maxLen);
}

export async function signInWithGoogle(): Promise<User> {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  const result = await signInWithPopup(auth, provider);
  await ensureUserProfile(result.user);
  return result.user;
}

export async function signOutUser(): Promise<void> {
  await signOut(auth);
}

export async function ensureUserProfile(user: User): Promise<void> {
  const uid = sanitizeId(user.uid);
  const userPath = `users/${uid}`;
  const userRef = doc(db, 'users', uid);

  try {
    const existing = await getDocFromServer(userRef);
    if (!existing.exists()) {
      await setDoc(userRef, {
        uid,
        displayName: clampStr(user.displayName ?? undefined, 120, 'Atelier Member'),
        email: clampStr(user.email ?? undefined, 200, 'member@atelier.local'),
        photoUrl: clampStr(user.photoURL ?? undefined, 500, ''),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    } else {
      await setDoc(
        userRef,
        {
          displayName: clampStr(user.displayName ?? undefined, 120, 'Atelier Member'),
          photoUrl: clampStr(user.photoURL ?? undefined, 500, ''),
          updatedAt: serverTimestamp(),
        },
        { merge: true }
      );
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, userPath);
  }
}

export async function saveGarmentAndLookToCloud(params: {
  user: User;
  garmentId?: string;
  analysis: GarmentAnalysis;
  brief: StyleBrief;
  sourceImageDataUrl: string;
  look: GeneratedLookState;
}): Promise<{ garmentId: string; lookId: string }> {
  const { user, analysis, brief, sourceImageDataUrl, look } = params;
  await ensureUserProfile(user);

  const uid = sanitizeId(user.uid);
  const garmentId = sanitizeId(params.garmentId || `garment_${Date.now()}`);
  const lookId = sanitizeId(look.savedLookId || `look_${look.occasion.toLowerCase().replace(/[^a-z0-9]/g, '_')}_${Date.now()}`);

  const garmentPath = `users/${uid}/garments/${garmentId}`;
  const lookPath = `users/${uid}/garments/${garmentId}/looks/${lookId}`;

  // Compress images cleanly under Firestore's strict 1MB document limit
  const compressedSource = await compressImageToDataUrl(sourceImageDataUrl, 900, 0.8, 340000);
  let compressedLookUrl: string | undefined;
  if (look.imageDataUrl) {
    const compressedLook = await compressImageToDataUrl(look.imageDataUrl, 960, 0.82, 440000);
    compressedLookUrl = compressedLook.dataUrl;
  }

  const safePalette = (analysis.palette || []).slice(0, 6).map((sw) => ({
    hex: clampStr(sw.hex, 20, '#141413'),
    name: clampStr(sw.name, 60, 'Neutral'),
    role: clampStr(sw.role, 40, 'Accent'),
  }));
  if (safePalette.length === 0) {
    safePalette.push({ hex: '#141413', name: 'Espresso Ink', role: 'Dominant' });
  }

  const safePieces = (look.pieces || []).slice(0, 10).map((p) => ({
    slot: clampStr(p.slot, 40, 'Piece'),
    name: clampStr(p.name, 120, 'Curated Item'),
    color: clampStr(p.color, 60, 'Neutral'),
    material: clampStr(p.material, 80, 'Woven'),
  }));
  if (safePieces.length === 0) {
    safePieces.push({
      slot: 'Anchor Piece',
      name: clampStr(analysis.garmentTitle, 120, 'Statement Piece'),
      color: 'Multi',
      material: 'Signature',
    });
  }

  const safeStyleInsight = analysis.styleInsight
    ? {
        fitAndProportion: clampStr(
          analysis.styleInsight.fitAndProportion,
          600,
          'Balance proportions against structured complements.'
        ),
        materialAndDrape: clampStr(
          analysis.styleInsight.materialAndDrape,
          600,
          'Pair with contrasting tactile weaves.'
        ),
        howToWear: clampStr(
          analysis.styleInsight.howToWear,
          600,
          'Anchor the silhouette cleanly at the waist or shoulder.'
        ),
        whatToAvoid: clampStr(
          analysis.styleInsight.whatToAvoid,
          600,
          'Avoid competing heavy patterns.'
        ),
      }
    : undefined;

  const safeQuickTips =
    Array.isArray(analysis.quickTips) && analysis.quickTips.length > 0
      ? analysis.quickTips.slice(0, 6).map((tip) => clampStr(tip, 200, 'Keep proportions balanced.'))
      : undefined;

  const garmentRef = doc(db, 'users', uid, 'garments', garmentId);
  try {
    const existingGarment = await getDocFromServer(garmentRef);
    if (!existingGarment.exists()) {
      const garmentDoc: Record<string, unknown> = {
        id: garmentId,
        ownerId: uid,
        title: clampStr(analysis.garmentTitle, 160, 'Curated Garment'),
        category: clampStr(analysis.category, 80, 'Apparel'),
        fabricNotes: clampStr(analysis.fabricNotes, 600, 'Textured finish'),
        styleSummary: clampStr(analysis.styleSummary, 1200, 'Editorial pairing anchor.'),
        season: brief.season,
        silhouette: brief.silhouette,
        budgetTier: brief.budgetTier,
        palette: safePalette,
        sourceImageDataUrl: compressedSource.dataUrl,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      };
      if (safeStyleInsight) {
        garmentDoc.styleInsight = safeStyleInsight;
      }
      if (safeQuickTips) {
        garmentDoc.quickTips = safeQuickTips;
      }
      await setDoc(garmentRef, garmentDoc);
    } else {
      const updateDocData: Record<string, unknown> = {
        title: clampStr(analysis.garmentTitle, 160, 'Curated Garment'),
        styleSummary: clampStr(analysis.styleSummary, 1200, 'Editorial pairing anchor.'),
        season: brief.season,
        silhouette: brief.silhouette,
        budgetTier: brief.budgetTier,
        updatedAt: serverTimestamp(),
      };
      if (safeStyleInsight) {
        updateDocData.styleInsight = safeStyleInsight;
      }
      if (safeQuickTips) {
        updateDocData.quickTips = safeQuickTips;
      }
      await setDoc(garmentRef, updateDocData, { merge: true });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, garmentPath);
  }

  const lookRef = doc(db, 'users', uid, 'garments', garmentId, 'looks', lookId);
  const safeHistory = (look.editPromptHistory || []).slice(0, 12).map((entry) => clampStr(entry, 300, 'Edit'));

  const lookPayload: Record<string, unknown> = {
    id: lookId,
    ownerId: uid,
    garmentId,
    occasion: look.occasion,
    title: clampStr(look.title, 160, `${look.occasion} Look`),
    stylingRationale: clampStr(look.stylingRationale, 1200, 'Balanced color and silhouette harmony.'),
    pieces: safePieces,
    editPromptHistory: safeHistory,
    updatedAt: serverTimestamp(),
  };

  if (compressedLookUrl) {
    lookPayload.flatLayImageDataUrl = compressedLookUrl;
  }

  try {
    const existingLook = await getDocFromServer(lookRef);
    if (!existingLook.exists()) {
      lookPayload.createdAt = serverTimestamp();
      await setDoc(lookRef, lookPayload);
    } else {
      await setDoc(lookRef, lookPayload, { merge: true });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, lookPath);
  }

  return { garmentId, lookId };
}

export function subscribeToWardrobeGarments(
  uid: string,
  onData: (garments: SavedGarmentRecord[]) => void,
  onError: (err: Error) => void
): Unsubscribe {
  const safeUid = sanitizeId(uid);
  const path = `users/${safeUid}/garments`;
  const q = query(collection(db, 'users', safeUid, 'garments'), orderBy('updatedAt', 'desc'));
  return onSnapshot(
    q,
    (snapshot) => {
      const items = snapshot.docs.map((d) => d.data() as SavedGarmentRecord);
      onData(items);
    },
    (error) => {
      try {
        handleFirestoreError(error, OperationType.LIST, path);
      } catch (wrapped) {
        onError(wrapped as Error);
      }
    }
  );
}

export function subscribeToGarmentLooks(
  uid: string,
  garmentId: string,
  onData: (looks: SavedLookRecord[]) => void,
  onError: (err: Error) => void
): Unsubscribe {
  const safeUid = sanitizeId(uid);
  const safeGid = sanitizeId(garmentId);
  const path = `users/${safeUid}/garments/${safeGid}/looks`;
  const q = query(
    collection(db, 'users', safeUid, 'garments', safeGid, 'looks'),
    orderBy('updatedAt', 'desc')
  );
  return onSnapshot(
    q,
    (snapshot) => {
      const items = snapshot.docs.map((d) => d.data() as SavedLookRecord);
      onData(items);
    },
    (error) => {
      try {
        handleFirestoreError(error, OperationType.LIST, path);
      } catch (wrapped) {
        onError(wrapped as Error);
      }
    }
  );
}

export async function deleteSavedLook(uid: string, garmentId: string, lookId: string): Promise<void> {
  const safeUid = sanitizeId(uid);
  const safeGid = sanitizeId(garmentId);
  const safeLid = sanitizeId(lookId);
  const path = `users/${safeUid}/garments/${safeGid}/looks/${safeLid}`;
  try {
    await deleteDoc(doc(db, 'users', safeUid, 'garments', safeGid, 'looks', safeLid));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, path);
  }
}

export async function deleteSavedGarment(uid: string, garmentId: string): Promise<void> {
  const safeUid = sanitizeId(uid);
  const safeGid = sanitizeId(garmentId);
  const path = `users/${safeUid}/garments/${safeGid}`;
  try {
    await deleteDoc(doc(db, 'users', safeUid, 'garments', safeGid));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, path);
  }
}
