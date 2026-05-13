/**
 * 공식 와일드리프트 KR 챔피언 페이지에서 스킨 이름 일괄 수집.
 *   npm run scrape-skins  →  server/data/champion-skins.json 생성
 *
 * 출력 구조:
 *   {
 *     fetchedAt: <ISO>,
 *     champions: {
 *       [alias]: { krName: "가렌", skins: ["데마시아의 힘 가렌", "공포의 기사 가렌", ...] }
 *     }
 *   }
 */
import { writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const INDEX_URL = "https://wildrift.leagueoflegends.com/ko-kr/champions/";
const UA = "Mozilla/5.0 (compatible; wr-spellcheck-scrape/1.0)";

interface ChampionEntry {
  alias: string;
  krName: string;
  skins: string[];
}

function extractNextData(html: string): any {
  const m = html.match(/<script[^>]*id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
  if (!m) throw new Error("__NEXT_DATA__ not found");
  return JSON.parse(m[1]);
}

async function fetchHtml(url: string): Promise<string> {
  const res = await fetch(url, { headers: { "User-Agent": UA } });
  if (!res.ok) throw new Error(`${url} -> ${res.status}`);
  return res.text();
}

async function fetchChampionList(): Promise<{ alias: string; krName: string }[]> {
  const html = await fetchHtml(INDEX_URL);
  const j = extractNextData(html);
  const items: any[] = j?.props?.pageProps?.page?.blades?.[2]?.items ?? [];
  const out: { alias: string; krName: string }[] = [];
  for (const it of items) {
    const url: string | undefined = it?.action?.payload?.url;
    const m = url?.match(/\/ko-kr\/champions\/([^/]+)/);
    if (!m) continue;
    out.push({ alias: m[1], krName: String(it.title) });
  }
  return out;
}

async function fetchSkins(alias: string): Promise<string[]> {
  const url = `https://wildrift.leagueoflegends.com/ko-kr/champions/${alias}/`;
  const html = await fetchHtml(url);
  const j = extractNextData(html);
  const blades: any[] = j?.props?.pageProps?.page?.blades ?? [];
  const skinBlade = blades.find(
    (b) => b?.type === "landingMediaCarousel" && b?.header?.title === "이용 가능 스킨",
  );
  if (!skinBlade) return [];
  const groups: any[] = skinBlade.groups ?? [];
  return groups.map((g) => String(g?.label ?? "")).filter(Boolean);
}

async function main() {
  console.log("[scrape] fetching champion list…");
  const list = await fetchChampionList();
  console.log(`[scrape] ${list.length} champions`);

  const result: Record<string, ChampionEntry> = {};
  let i = 0;
  for (const { alias, krName } of list) {
    i++;
    try {
      const skins = await fetchSkins(alias);
      result[alias] = { alias, krName, skins };
      console.log(`  [${i}/${list.length}] ${krName.padEnd(8)} (${alias}) — ${skins.length} skins`);
    } catch (e) {
      console.warn(`  [${i}/${list.length}] ${alias} FAIL: ${e}`);
      result[alias] = { alias, krName, skins: [] };
    }
    // 가벼운 polite delay
    await new Promise((r) => setTimeout(r, 100));
  }

  const here = dirname(fileURLToPath(import.meta.url));
  const out = resolve(here, "../data/champion-skins.json");
  const payload = {
    fetchedAt: new Date().toISOString(),
    champions: result,
  };
  await writeFile(out, JSON.stringify(payload, null, 2) + "\n", "utf8");
  const totalSkins = Object.values(result).reduce((a, c) => a + c.skins.length, 0);
  console.log(`[scrape] done. ${Object.keys(result).length} champs, ${totalSkins} skin names → ${out}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
