/**
 * 나무위키 챔피언 스킨 페이지에서 추가 스킨 이름 수집 → 기존 champion-skins.json에 union 머지.
 *
 *   npm run scrape-skins-namu
 *
 * URL 패턴:
 *   1차: https://namu.wiki/w/<챔피언명>/스킨
 *   2차(404): https://namu.wiki/w/<챔피언명>(리그%20오브%20레전드)/스킨
 *
 * 파싱: TOC의 <a href='#s-X.Y'>X.Y</a>. <title>...</title> 패턴에서 title 추출.
 *       section label("기본 스킨", "보유 스킨", "와일드 리프트 오리지널 스킨", "크로마", "개요" 등)은 제외.
 */
import { readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const UA =
  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) wr-spellcheck-scrape/1.0";

// 스킨이 아닌 섹션 라벨 — TOC에 등장하지만 스킨명으로 취급 안 함.
const NON_SKIN_LABELS = new Set([
  "개요",
  "기본 스킨",
  "보유 스킨",
  "와일드 리프트 오리지널 스킨",
  "크로마",
  "관련 문서",
  "기타",
  "관련 게시물",
  "여담",
  "출시 전",
  "출시 후",
  "스킨 컬렉션",
  "스킨 트레일러",
  "단종된 스킨",
  "한정 스킨",
  "한정판 스킨",
  "이벤트 스킨",
  "전설의 스킨",
  "한정",
  "PBE 변경사항",
]);

interface NamuTocEntry {
  section: string;
  title: string;
}

function parseToc(html: string): NamuTocEntry[] {
  const pat = /href='#s-(\d+(?:\.\d+)*)'[^>]*>\1<\/a>\.\s*([\s\S]*?)<\/span>/g;
  const out: NamuTocEntry[] = [];
  let m: RegExpExecArray | null;
  while ((m = pat.exec(html)) !== null) {
    const section = m[1];
    const titleHtml = m[2];
    const title = titleHtml
      .replace(/<[^>]+>/g, "")
      .replace(/\s+/g, " ")
      .trim();
    if (title) out.push({ section, title });
  }
  return out;
}

async function fetchHtml(url: string): Promise<{ html: string; ok: boolean }> {
  const res = await fetch(url, {
    headers: { "User-Agent": UA, "Accept-Language": "ko-KR,ko;q=0.9" },
  });
  if (!res.ok) return { html: "", ok: false };
  const html = await res.text();
  // 빈 페이지/리다이렉트는 본문에 스킨 섹션이 없다 — TOC 파싱 결과로 판별.
  return { html, ok: true };
}

async function fetchSkinNamesForChampion(krName: string): Promise<string[]> {
  const encoded = encodeURIComponent(`${krName}/스킨`);
  const url1 = `https://namu.wiki/w/${encoded}`;
  const r1 = await fetchHtml(url1);
  let entries = r1.ok ? parseToc(r1.html) : [];
  // 스킨 페이지가 비어있거나 "개요"만 있으면 disambiguation suffix 시도.
  if (entries.filter((e) => !NON_SKIN_LABELS.has(e.title)).length === 0) {
    const encoded2 = encodeURIComponent(`${krName}(리그 오브 레전드)/스킨`);
    const url2 = `https://namu.wiki/w/${encoded2}`;
    const r2 = await fetchHtml(url2);
    if (r2.ok) entries = parseToc(r2.html);
  }
  return entries
    .map((e) => e.title)
    .filter((t) => t && !NON_SKIN_LABELS.has(t));
}

interface ExistingFile {
  fetchedAt: string;
  champions: Record<string, { alias: string; krName: string; skins: string[] }>;
}

async function main() {
  const here = dirname(fileURLToPath(import.meta.url));
  const krPath = resolve(here, "../data/champion-kr.json");
  const skinsPath = resolve(here, "../data/champion-skins.json");

  const krMap = JSON.parse(await readFile(krPath, "utf8")) as Record<string, string>;
  const existing = JSON.parse(await readFile(skinsPath, "utf8")) as ExistingFile;

  // krName → alias 역색인 (existing 데이터 기반)
  const krToAlias = new Map<string, string>();
  for (const c of Object.values(existing.champions)) {
    krToAlias.set(c.krName, c.alias);
  }

  // 모든 한국명 (krMap 기반)
  const allKrNames = Array.from(new Set(Object.values(krMap)));
  console.log(`[namu] ${allKrNames.length} champions in champion-kr.json`);

  let added = 0;
  let i = 0;
  for (const krName of allKrNames) {
    i++;
    try {
      const namuSkins = await fetchSkinNamesForChampion(krName);
      if (namuSkins.length === 0) {
        console.log(`  [${i}/${allKrNames.length}] ${krName.padEnd(10)} — namu: 0 (skip)`);
        await new Promise((r) => setTimeout(r, 300));
        continue;
      }
      const alias = krToAlias.get(krName);
      let entry = alias ? existing.champions[alias] : undefined;
      if (!entry) {
        // existing에 없는 챔피언 — 새 alias 슬롯 추가 (krName 기준)
        const newAlias = `kr-${encodeURIComponent(krName)}`;
        entry = { alias: newAlias, krName, skins: [] };
        existing.champions[newAlias] = entry;
      }
      const before = new Set(entry.skins);
      const merged = new Set<string>(entry.skins);
      for (const s of namuSkins) merged.add(s);
      entry.skins = Array.from(merged);
      const newCount = entry.skins.length - before.size;
      added += newCount;
      console.log(
        `  [${i}/${allKrNames.length}] ${krName.padEnd(10)} — namu: ${namuSkins.length}, +${newCount} new (total ${entry.skins.length})`,
      );
    } catch (e) {
      console.warn(`  [${i}/${allKrNames.length}] ${krName} FAIL: ${e}`);
    }
    await new Promise((r) => setTimeout(r, 300));
  }

  existing.fetchedAt = new Date().toISOString();
  await writeFile(skinsPath, JSON.stringify(existing, null, 2) + "\n", "utf8");
  const totalSkins = Object.values(existing.champions).reduce((a, c) => a + c.skins.length, 0);
  console.log(`[namu] done. +${added} new skin names. Total skins now: ${totalSkins}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
