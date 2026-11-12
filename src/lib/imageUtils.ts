export interface ParsedImageData {
  dataUrl: string;
  base64: string;
  mimeType: string;
}

export function splitDataUrl(dataUrl: string): { base64: string; mimeType: string } {
  const match = /^data:([^;]+);base64,(.+)$/.exec(dataUrl);
  if (!match) {
    return { base64: dataUrl, mimeType: 'image/jpeg' };
  }
  return {
    mimeType: match[1] || 'image/jpeg',
    base64: match[2] || '',
  };
}

export async function compressImageToDataUrl(
  source: File | Blob | string,
  maxDimension = 1024,
  quality = 0.84,
  maxChars = 420000
): Promise<ParsedImageData> {
  let rawUrl: string;
  let shouldRevoke = false;

  if (typeof source === 'string') {
    rawUrl = source;
  } else {
    rawUrl = URL.createObjectURL(source);
    shouldRevoke = true;
  }

  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const image = new Image();
      image.crossOrigin = 'anonymous';
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error('Could not decode image file.'));
      image.src = rawUrl;
    });

    const scale = Math.min(1, maxDimension / Math.max(img.width, img.height));
    let width = Math.max(1, Math.round(img.width * scale));
    let height = Math.max(1, Math.round(img.height * scale));

    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Canvas 2D context is unavailable.');
    }

    let currentQuality = quality;
    let dataUrl = '';

    for (let attempt = 0; attempt < 4; attempt++) {
      canvas.width = width;
      canvas.height = height;
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(0, 0, width, height);
      ctx.drawImage(img, 0, 0, width, height);
      dataUrl = canvas.toDataURL('image/jpeg', currentQuality);
      if (dataUrl.length <= maxChars) {
        break;
      }
      width = Math.max(320, Math.round(width * 0.8));
      height = Math.max(320, Math.round(height * 0.8));
      currentQuality = Math.max(0.6, currentQuality - 0.08);
    }

    const { base64, mimeType } = splitDataUrl(dataUrl);
    return { dataUrl, base64, mimeType };
  } finally {
    if (shouldRevoke) {
      URL.revokeObjectURL(rawUrl);
    }
  }
}

export async function urlToCompressedDataUrl(
  url: string,
  maxDimension = 1024,
  quality = 0.86
): Promise<ParsedImageData> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Unable to load sample garment (${response.status})`);
  }
  const blob = await response.blob();
  return compressImageToDataUrl(blob, maxDimension, quality);
}

export function triggerImageDownload(dataUrl: string, filename: string): void {
  const link = document.createElement('a');
  link.href = dataUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

export async function triggerJpegDownload(
  dataUrl: string,
  filename: string,
  quality = 0.92
): Promise<void> {
  const jpegFilename = filename.replace(/\.(png|webp|jpeg|jpg)$/i, '') + '.jpg';

  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const image = new Image();
      image.crossOrigin = 'anonymous';
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error('Could not decode image for JPEG export.'));
      image.src = dataUrl;
    });

    const canvas = document.createElement('canvas');
    canvas.width = img.naturalWidth || img.width;
    canvas.height = img.naturalHeight || img.height;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      triggerImageDownload(dataUrl, jpegFilename);
      return;
    }

    ctx.fillStyle = '#FFFFFF';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(img, 0, 0);

    const jpegDataUrl = canvas.toDataURL('image/jpeg', quality);
    triggerImageDownload(jpegDataUrl, jpegFilename);
  } catch {
    triggerImageDownload(dataUrl, jpegFilename);
  }
}

export interface ExtractedSwatch {
  hex: string;
  label: string;
}

const FASHION_COLOR_KEYWORDS: Array<[RegExp, string]> = [
  [/\b(ivory|ecru|cream|alabaster|off[- ]?white|bone|chalk)\b/i, '#F3EFE6'],
  [/\b(white|snow|optic)\b/i, '#FAFAFA'],
  [/\b(espresso|dark brown|chocolate|mocha|mahogany)\b/i, '#3E2B23'],
  [/\b(cognac|saddle|tan|chestnut|caramel|tobacco)\b/i, '#9C6137'],
  [/\b(camel|oat|sand|beige|stone|taupe|khaki|linen)\b/i, '#C9B396'],
  [/\b(black|onyx|obsidian|jet|noir)\b/i, '#181818'],
  [/\b(charcoal|slate|graphite|anthracite|smoke)\b/i, '#44464A'],
  [/\b(gray|grey|ash|pebble)\b/i, '#8E9196'],
  [/\b(silver|pewter|chrome|platinum|steel)\b/i, '#BFC4CA'],
  [/\b(gold|brass|champagne|gilt)\b/i, '#B7934F'],
  [/\b(navy|midnight)\b/i, '#1D2944'],
  [/\b(indigo|denim|cobalt|royal blue)\b/i, '#2E4775'],
  [/\b(sky|powder blue|chambray|ice blue)\b/i, '#9AB4CE'],
  [/\b(blue)\b/i, '#3D5E8C'],
  [/\b(burgundy|oxblood|wine|merlot|bordeaux|maroon)\b/i, '#5E202B'],
  [/\b(crimson|ruby|scarlet|cherry|red)\b/i, '#9E2A2F'],
  [/\b(terracotta|rust|burnt orange|sienna|copper)\b/i, '#A84E32'],
  [/\b(blush|rose|dusty pink|mauve|pink)\b/i, '#D6A6A8'],
  [/\b(olive|moss|army)\b/i, '#5C6643'],
  [/\b(sage|eucalyptus|celadon|mint)\b/i, '#8EA18B'],
  [/\b(emerald|forest|hunter|pine|green)\b/i, '#274E3D'],
  [/\b(mustard|ochre|amber|saffron|yellow)\b/i, '#C49438'],
  [/\b(plum|aubergine|eggplant|violet|purple|lavender)\b/i, '#57365B'],
];

export function mapFashionColorNameToHex(colorName: string, index = 0): string {
  for (const [pattern, hex] of FASHION_COLOR_KEYWORDS) {
    if (pattern.test(colorName)) {
      return hex;
    }
  }
  const editorialFallbacks = ['#1F1E1C', '#8A6A42', '#5A5852', '#C5BFB3', '#3D4856'];
  return editorialFallbacks[index % editorialFallbacks.length];
}

export function extractSwatchesFromPieces(
  pieces: Array<{ color: string; name?: string }>
): ExtractedSwatch[] {
  const seen = new Set<string>();
  const result: ExtractedSwatch[] = [];

  pieces.forEach((piece, idx) => {
    const label = (piece.color || '').trim();
    if (!label) return;
    const hex = mapFashionColorNameToHex(label, idx);
    if (!seen.has(hex)) {
      seen.add(hex);
      result.push({ hex, label });
    }
  });

  return result.slice(0, 5);
}

function rgbToHex(r: number, g: number, b: number): string {
  const toHex = (n: number) => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, '0');
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`.toUpperCase();
}

export async function extractDominantColorsFromDataUrl(
  dataUrl: string,
  maxColors = 5
): Promise<ExtractedSwatch[]> {
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const image = new Image();
      image.crossOrigin = 'anonymous';
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error('Image load failed'));
      image.src = dataUrl;
    });

    const sampleSize = 56;
    const canvas = document.createElement('canvas');
    canvas.width = sampleSize;
    canvas.height = sampleSize;
    const ctx = canvas.getContext('2d');
    if (!ctx) return [];

    ctx.drawImage(img, 0, 0, sampleSize, sampleSize);
    const { data } = ctx.getImageData(0, 0, sampleSize, sampleSize);

    const buckets = new Map<string, { r: number; g: number; b: number; weight: number }>();

    // Sample center-weighted pixels and exclude border studio paper background
    for (let y = 2; y < sampleSize - 2; y++) {
      for (let x = 2; x < sampleSize - 2; x++) {
        const idx = (y * sampleSize + x) * 4;
        const a = data[idx + 3];
        if (a < 180) continue;

        const r = data[idx];
        const g = data[idx + 1];
        const b = data[idx + 2];

        const maxCh = Math.max(r, g, b);
        const minCh = Math.min(r, g, b);
        const chroma = maxCh - minCh;

        // Skip crisp white/alabaster gallery backdrop
        if (r > 228 && g > 224 && b > 214 && chroma < 24) {
          continue;
        }

        // Quantize into 24-step buckets for crisp perceptual clustering
        const qr = Math.round(r / 24) * 24;
        const qg = Math.round(g / 24) * 24;
        const qb = Math.round(b / 24) * 24;
        const key = `${qr},${qg},${qb}`;

        // Slightly prioritize colorful garment accents while preserving deep neutrals
        const pixelWeight = 1 + chroma / 110;
        const existing = buckets.get(key);
        if (existing) {
          existing.r += r * pixelWeight;
          existing.g += g * pixelWeight;
          existing.b += b * pixelWeight;
          existing.weight += pixelWeight;
        } else {
          buckets.set(key, {
            r: r * pixelWeight,
            g: g * pixelWeight,
            b: b * pixelWeight,
            weight: pixelWeight,
          });
        }
      }
    }

    const candidates = Array.from(buckets.values())
      .map((entry) => ({
        r: entry.r / entry.weight,
        g: entry.g / entry.weight,
        b: entry.b / entry.weight,
        weight: entry.weight,
      }))
      .sort((a, b) => b.weight - a.weight);

    const chosen: Array<{ r: number; g: number; b: number }> = [];
    for (const cand of candidates) {
      const tooClose = chosen.some(
        (c) => Math.hypot(c.r - cand.r, c.g - cand.g, c.b - cand.b) < 42
      );
      if (!tooClose) {
        chosen.push({ r: cand.r, g: cand.g, b: cand.b });
        if (chosen.length >= maxColors) break;
      }
    }

    return chosen.map((c) => {
      const hex = rgbToHex(c.r, c.g, c.b);
      return { hex, label: hex };
    });
  } catch {
    return [];
  }
}
