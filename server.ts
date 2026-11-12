import 'dotenv/config';
import express, { Request, Response } from 'express';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { GoogleGenAI, Type, GenerateContentResponse } from '@google/genai';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = Number(process.env.PORT) || 3000;

function getAiClient(): GoogleGenAI {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY is not configured in environment secrets.');
  }
  return new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

function classifyGeminiError(err: unknown): {
  status: number;
  message: string;
  code: string;
  requiresPaidKey: boolean;
} {
  const raw = err instanceof Error ? err.message : String(err);
  const lower = raw.toLowerCase();

  if (lower.includes('api_key_invalid') || lower.includes('api key not valid')) {
    return {
      status: 400,
      code: 'API_KEY_INVALID',
      requiresPaidKey: false,
      message: 'Your Gemini API key appears invalid. Please check it in Settings > Secrets.',
    };
  }
  if (lower.includes('permission_denied') || lower.includes('403')) {
    return {
      status: 403,
      code: 'PERMISSION_DENIED',
      requiresPaidKey: true,
      message:
        'Permission denied by Gemini API. If using gemini-nano-banana-2.1, please ensure a billing-enabled API key is selected in Settings > Secrets.',
    };
  }
  if (lower.includes('resource_exhausted') || lower.includes('429') || lower.includes('quota')) {
    return {
      status: 429,
      code: 'RESOURCE_EXHAUSTED',
      requiresPaidKey: true,
      message:
        'Gemini API quota exhausted for this model. Flat-lay image generation with gemini-nano-banana-2.1 requires a paid-tier API key in Settings > Secrets.',
    };
  }
  if (lower.includes('not_found') || lower.includes('404')) {
    return {
      status: 404,
      code: 'MODEL_NOT_FOUND',
      requiresPaidKey: false,
      message: 'Requested Gemini model was not found or is currently unavailable.',
    };
  }

  return {
    status: 500,
    code: 'GEMINI_ERROR',
    requiresPaidKey: false,
    message: raw || 'Failed to communicate with Gemini API.',
  };
}

function extractImageFromResponse(response: GenerateContentResponse): {
  imageDataUrl: string | null;
  caption: string;
} {
  let imageDataUrl: string | null = null;
  const textParts: string[] = [];

  for (const part of response.candidates?.[0]?.content?.parts ?? []) {
    if (part.inlineData?.data) {
      const mime = part.inlineData.mimeType ?? 'image/png';
      imageDataUrl = `data:${mime};base64,${part.inlineData.data}`;
    } else if (part.text) {
      textParts.push(part.text.trim());
    }
  }

  return {
    imageDataUrl,
    caption: textParts.join(' ').trim(),
  };
}

async function startServer() {
  const app = express();
  app.use(express.json({ limit: '25mb' }));
  app.use('/api/samples', express.static(path.resolve(__dirname, 'src/assets/images')));

  app.get('/api/health', (_req: Request, res: Response) => {
    res.json({ ok: true });
  });

  app.get('/api/apk/atelier-virtual-stylist.apk', (_req: Request, res: Response) => {
    const apkPath = path.resolve(
      __dirname,
      'android/app/build/outputs/apk/debug/app-debug.apk'
    );
    if (!fs.existsSync(apkPath)) {
      res.status(404).json({ error: 'APK has not been built yet.' });
      return;
    }
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader(
      'Content-Disposition',
      'attachment; filename="atelier-virtual-stylist.apk"'
    );
    res.sendFile(apkPath);
  });

  // 1. Analyze uploaded garment & create 3 structured outfit blueprints (Casual, Business, Night Out)
  app.post('/api/stylist/analyze', async (req: Request, res: Response) => {
    try {
      const {
        imageBase64,
        mimeType = 'image/jpeg',
        season = 'Year-Round',
        silhouette = 'Tailored',
        budgetTier = 'Contemporary',
      } = req.body ?? {};

      if (!imageBase64 || typeof imageBase64 !== 'string') {
        res.status(400).json({ error: 'Garment image data is required.' });
        return;
      }

      const ai = getAiClient();

      const prompt = `You are a senior luxury fashion editorial stylist. Analyze the uploaded garment photo carefully.
Identify:
1. Its exact garment title, category, fabric/weave/pattern notes, and 2-3 sentences of editorial styling principles explaining how to balance its color, pattern scale, and proportion.
2. Its true color palette: 4 to 5 exact hex swatches with poetic fashion color names and clear tonal roles in the look ("Primary", "Secondary", "Accent", "Neutral Bridge", or "Trim").
3. Actionable "styleInsight" advice tailored specifically to this garment's material and cut:
   - fitAndProportion: Practical advice on silhouette balance, tucking, layering, or hemline/shoulder alignment for this cut.
   - materialAndDrape: How the fabric weight, sheen, or texture behaves and which contrasting textures pair best.
   - howToWear: Specific, wearable styling move (waist definition, cuff/collar styling, footwear proportion).
   - whatToAvoid: One clear styling pitfall to skip with this material or cut.
4. "quickTips": 4 to 5 ultra-concise, actionable one-line styling tips (under 14 words each, e.g. "Tuck in for a cleaner silhouette" or "Pair with brushed metallic accessories") specifically tailored to this garment's texture and cut.
5. Three complete, distinct outfit recipes featuring THIS exact garment as the centerpiece, tailored to the user's brief:
   - Season: ${season}
   - Preferred Silhouette: ${silhouette}
   - Budget Tier: ${budgetTier}

The three occasions MUST be in order:
1) "Casual" — effortless daytime / gallery weekend
2) "Business" — sharp studio / executive polish
3) "Night Out" — elevated evening / cocktail sophistication

For each outfit, provide:
- An evocative editorial look title (2-4 words)
- A 2-sentence styling rationale explaining why the proportions, materials, and color bridges work
- 4 to 5 itemized pieces (the first piece MUST be this exact uploaded garment with slot "Anchor Piece", followed by complementary slots like "Top", "Outerwear", "Footwear", "Bag", "Accessories")
- A rich art-direction description ("flatLayDirection") for an overhead flat-lay stylist describing the exact visual arrangement, fabric textures, colors, and folded placement of every piece on crisp gallery alabaster paper so an image generator renders a cohesive, realistic editorial flat-lay.`;

      const response: GenerateContentResponse = await ai.models.generateContent({
        model: 'gemini-4-hydrogen',
        contents: {
          parts: [
            {
              inlineData: {
                mimeType,
                data: imageBase64,
              },
            },
            { text: prompt },
          ],
        },
        config: {
          systemInstruction:
            'You are an editorial fashion director at a minimalist luxury atelier. Output concise, specific, wearable garment pairings grounded in real color theory, tailoring, and textile behavior.',
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              garmentTitle: { type: Type.STRING },
              category: { type: Type.STRING },
              fabricNotes: { type: Type.STRING },
              styleSummary: { type: Type.STRING },
              palette: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    hex: { type: Type.STRING },
                    name: { type: Type.STRING },
                    role: { type: Type.STRING },
                  },
                  required: ['hex', 'name', 'role'],
                },
              },
              styleInsight: {
                type: Type.OBJECT,
                properties: {
                  fitAndProportion: { type: Type.STRING },
                  materialAndDrape: { type: Type.STRING },
                  howToWear: { type: Type.STRING },
                  whatToAvoid: { type: Type.STRING },
                },
                required: ['fitAndProportion', 'materialAndDrape', 'howToWear', 'whatToAvoid'],
              },
              quickTips: {
                type: Type.ARRAY,
                items: { type: Type.STRING },
              },
              outfits: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    occasion: { type: Type.STRING },
                    title: { type: Type.STRING },
                    stylingRationale: { type: Type.STRING },
                    flatLayDirection: { type: Type.STRING },
                    pieces: {
                      type: Type.ARRAY,
                      items: {
                        type: Type.OBJECT,
                        properties: {
                          slot: { type: Type.STRING },
                          name: { type: Type.STRING },
                          color: { type: Type.STRING },
                          material: { type: Type.STRING },
                        },
                        required: ['slot', 'name', 'color', 'material'],
                      },
                    },
                  },
                  required: ['occasion', 'title', 'stylingRationale', 'flatLayDirection', 'pieces'],
                },
              },
            },
            required: [
              'garmentTitle',
              'category',
              'fabricNotes',
              'styleSummary',
              'palette',
              'styleInsight',
              'quickTips',
              'outfits',
            ],
          },
        },
      });

      const rawText = (response.text ?? '{}').trim();
      const parsed = JSON.parse(rawText);
      res.json(parsed);
    } catch (err) {
      const classified = classifyGeminiError(err);
      res.status(classified.status).json({
        error: classified.message,
        code: classified.code,
        requiresPaidKey: classified.requiresPaidKey,
      });
    }
  });

  // 1b. On-demand Style Insight generation based on material and cut
  app.post('/api/stylist/style-insight', async (req: Request, res: Response) => {
    try {
      const {
        imageBase64,
        mimeType = 'image/jpeg',
        garmentTitle = 'Statement Garment',
        category = 'Apparel',
        fabricNotes = '',
        season = 'Year-Round',
        silhouette = 'Tailored',
      } = req.body ?? {};

      if (!imageBase64 && !fabricNotes) {
        res.status(400).json({ error: 'Garment image or fabric notes are required.' });
        return;
      }

      const ai = getAiClient();
      const parts: Array<{ inlineData?: { mimeType: string; data: string }; text?: string }> = [];

      if (imageBase64 && typeof imageBase64 === 'string') {
        parts.push({
          inlineData: {
            mimeType,
            data: imageBase64,
          },
        });
      }

      parts.push({
        text: `Provide concise, practical Style Insight advice for wearing this specific garment (${garmentTitle}, category: ${category}) tailored to a ${season} season and ${silhouette} silhouette preference.
Known material & cut details: ${fabricNotes || 'Inspect the garment photo for fabric weight, drape, texture, and tailoring cut.'}

Return 4 crisp, actionable insights (2 sentences max each):
- fitAndProportion: How to balance the garment's cut (waistline, volume, hemline, or shoulder structure).
- materialAndDrape: How to work with its fabric weight/finish and what contrasting textures complement it.
- howToWear: Specific styling execution (tucking, belting, layering, footwear proportion).
- whatToAvoid: One concrete styling pitfall to skip with this cut and material.`,
      });

      const response: GenerateContentResponse = await ai.models.generateContent({
        model: 'gemini-4-hydrogen',
        contents: { parts },
        config: {
          systemInstruction:
            'You are a master tailor and editorial wardrobe consultant. Give crisp, practical wearing advice grounded in garment construction, fabric drape, and proportion.',
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              fitAndProportion: { type: Type.STRING },
              materialAndDrape: { type: Type.STRING },
              howToWear: { type: Type.STRING },
              whatToAvoid: { type: Type.STRING },
            },
            required: ['fitAndProportion', 'materialAndDrape', 'howToWear', 'whatToAvoid'],
          },
        },
      });

      const parsed = JSON.parse((response.text ?? '{}').trim());
      res.json(parsed);
    } catch (err) {
      const classified = classifyGeminiError(err);
      res.status(classified.status).json({
        error: classified.message,
        code: classified.code,
        requiresPaidKey: classified.requiresPaidKey,
      });
    }
  });

  // 1c. On-demand Quick Tips overlay generation based on texture and cut
  app.post('/api/stylist/quick-tips', async (req: Request, res: Response) => {
    try {
      const {
        imageBase64,
        mimeType = 'image/jpeg',
        garmentTitle = 'Statement Garment',
        category = 'Apparel',
        fabricNotes = '',
        season = 'Year-Round',
        silhouette = 'Tailored',
      } = req.body ?? {};

      if (!imageBase64 && !fabricNotes) {
        res.status(400).json({ error: 'Garment image or fabric notes are required.' });
        return;
      }

      const ai = getAiClient();
      const parts: Array<{ inlineData?: { mimeType: string; data: string }; text?: string }> = [];

      if (imageBase64 && typeof imageBase64 === 'string') {
        parts.push({
          inlineData: {
            mimeType,
            data: imageBase64,
          },
        });
      }

      parts.push({
        text: `Generate 5 ultra-concise, actionable Quick Tips for styling "${garmentTitle}" (${category}, ${season}, ${silhouette} silhouette).
Texture and cut notes: ${fabricNotes || 'Inspect the garment photo for texture, weave, and cut.'}

Rules:
- Each tip must be a crisp, imperative styling cue (maximum 14 words), e.g., "Tuck in for a cleaner silhouette" or "Pair with brushed metallic accessories".
- Focus strictly on texture contrast, waist/hem proportion, sleeve/collar styling, and accessory pairing for this cut.`,
      });

      const response: GenerateContentResponse = await ai.models.generateContent({
        model: 'gemini-4-hydrogen',
        contents: { parts },
        config: {
          systemInstruction:
            'You are a runway wardrobe stylist. Output short, punchy, immediately actionable styling cues.',
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              quickTips: {
                type: Type.ARRAY,
                items: { type: Type.STRING },
              },
            },
            required: ['quickTips'],
          },
        },
      });

      const parsed = JSON.parse((response.text ?? '{"quickTips":[]}').trim());
      res.json(parsed);
    } catch (err) {
      const classified = classifyGeminiError(err);
      res.status(classified.status).json({
        error: classified.message,
        code: classified.code,
        requiresPaidKey: classified.requiresPaidKey,
      });
    }
  });

  // 2. Generate clean overhead flat-lay photograph featuring the uploaded item via gemini-nano-banana-2.1
  app.post('/api/stylist/generate-flatlay', async (req: Request, res: Response) => {
    try {
      const {
        garmentImageBase64,
        garmentMimeType = 'image/jpeg',
        occasion = 'Casual',
        garmentTitle = 'Statement Garment',
        lookTitle = 'Editorial Look',
        flatLayDirection = '',
        pieces = [],
        season = 'Year-Round',
        silhouette = 'Tailored',
      } = req.body ?? {};

      if (!garmentImageBase64 || typeof garmentImageBase64 !== 'string') {
        res.status(400).json({ error: 'Anchor garment image is required.' });
        return;
      }

      const piecesSummary = Array.isArray(pieces)
        ? pieces
            .map(
              (p: { slot?: string; name?: string; color?: string; material?: string }) =>
                `- ${p.slot ?? 'Piece'}: ${p.name ?? ''} (${p.color ?? ''}, ${p.material ?? ''})`
            )
            .join('\n')
        : '';

      const prompt = `Create a high-fashion overhead 90-degree flat-lay studio photograph for a "${occasion}" outfit titled "${lookTitle}" (${season}, ${silhouette} silhouette).

CRITICAL REQUIREMENT: The flat-lay MUST prominently feature the EXACT garment shown in the reference photo (${garmentTitle}), preserving its exact colors, pattern, silhouette, and fabric details without altering its design.

Arrange the complete outfit neatly on a clean, crisp warm-white alabaster gallery paper / matte limestone surface with balanced editorial spacing and soft natural side-window shadows. No mannequins, no human models, no text labels, no watermarks.

Complete pieces to arrange together in the flat-lay:
${piecesSummary}

Art direction:
${flatLayDirection}

Photography style: Shot from directly overhead on a medium-format camera, tactile fabric textures, crisp folds, luxury editorial lookbook presentation.`;

      const ai = getAiClient();
      const response: GenerateContentResponse = await ai.models.generateContent({
        model: 'gemini-nano-banana-2.1',
        contents: {
          parts: [
            {
              inlineData: {
                mimeType: garmentMimeType,
                data: garmentImageBase64,
              },
            },
            { text: prompt },
          ],
        },
        config: {
          imageConfig: {
            aspectRatio: '3:4',
            imageSize: '1K',
          },
        },
      });

      const { imageDataUrl, caption } = extractImageFromResponse(response);
      if (!imageDataUrl) {
        res.status(502).json({
          error: 'The image model completed without returning an image frame. Please try again.',
          code: 'EMPTY_IMAGE_RESPONSE',
          requiresPaidKey: false,
        });
        return;
      }

      res.json({ imageDataUrl, caption, model: 'gemini-nano-banana-2.1' });
    } catch (err) {
      const classified = classifyGeminiError(err);
      res.status(classified.status).json({
        error: classified.message,
        code: classified.code,
        requiresPaidKey: classified.requiresPaidKey,
      });
    }
  });

  // 3. Create or edit flat-lay images from text prompts via gemini-nano-banana-2.1
  app.post('/api/stylist/edit-flatlay', async (req: Request, res: Response) => {
    try {
      const {
        baseImageBase64,
        baseMimeType = 'image/png',
        sourceGarmentBase64,
        sourceGarmentMimeType = 'image/jpeg',
        editPrompt,
        aspectRatio = '3:4',
        imageSize = '1K',
      } = req.body ?? {};

      if (!editPrompt || typeof editPrompt !== 'string' || !editPrompt.trim()) {
        res.status(400).json({ error: 'A text prompt is required to create or edit a flat-lay.' });
        return;
      }

      const parts: Array<{ inlineData?: { mimeType: string; data: string }; text?: string }> = [];

      if (baseImageBase64 && typeof baseImageBase64 === 'string') {
        parts.push({
          inlineData: {
            mimeType: baseMimeType,
            data: baseImageBase64,
          },
        });
      }

      if (sourceGarmentBase64 && typeof sourceGarmentBase64 === 'string') {
        parts.push({
          inlineData: {
            mimeType: sourceGarmentMimeType,
            data: sourceGarmentBase64,
          },
        });
      }

      const fullPrompt = baseImageBase64
        ? `Edit the provided overhead flat-lay fashion photograph according to the stylist instruction below. Keep the overhead 90-degree flat-lay composition on crisp gallery alabaster paper, maintain realistic lighting and shadows, preserve the anchor garment's design${sourceGarmentBase64 ? ' (shown in the second reference image)' : ''}, and cleanly apply only the requested changes. No text, labels, or mannequins.\n\nStylist edit request: ${editPrompt.trim()}`
        : `Generate a high-fashion overhead 90-degree flat-lay outfit photograph on crisp gallery alabaster paper with soft natural window shadows. No mannequins, no human models, no text overlays.\n\nOutfit composition brief: ${editPrompt.trim()}`;

      parts.push({ text: fullPrompt });

      const allowedAspectRatios = ['1:1', '3:4', '4:3', '9:16', '16:9'];
      const safeAspect = allowedAspectRatios.includes(aspectRatio) ? aspectRatio : '3:4';
      const allowedSizes = ['1K', '2K'];
      const safeSize = allowedSizes.includes(imageSize) ? imageSize : '1K';

      const ai = getAiClient();
      const response: GenerateContentResponse = await ai.models.generateContent({
        model: 'gemini-nano-banana-2.1',
        contents: { parts },
        config: {
          imageConfig: {
            aspectRatio: safeAspect,
            imageSize: safeSize,
          },
        },
      });

      const { imageDataUrl, caption } = extractImageFromResponse(response);
      if (!imageDataUrl) {
        res.status(502).json({
          error: 'The image model did not return an image. Try refining your prompt.',
          code: 'EMPTY_IMAGE_RESPONSE',
          requiresPaidKey: false,
        });
        return;
      }

      res.json({ imageDataUrl, caption, model: 'gemini-nano-banana-2.1' });
    } catch (err) {
      const classified = classifyGeminiError(err);
      res.status(classified.status).json({
        error: classified.message,
        code: classified.code,
        requiresPaidKey: classified.requiresPaidKey,
      });
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req: Request, res: Response) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Atelier Virtual Stylist server listening on http://0.0.0.0:${PORT}`);
  });
}

startServer().catch((err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
