import { TencentHero, TierTableByCohort, buildTierTable, fetchHeroList } from "./fetcher/tencent.js";
import { NewsItem, fetchLatestNews } from "./fetcher/news.js";
import krMap from "../data/champion-kr.json" with { type: "json" };

let cachedTier: TierTableByCohort | null = null;
let cachedHeroes: Record<string, TencentHero> | null = null;
let cachedNews: { item: NewsItem; fetchedAt: number } | null = null;
const NEWS_TTL_MS = 24 * 60 * 60 * 1000; // 24시간

export async function getLatestNews(): Promise<{ item: NewsItem; fetchedAt: number }> {
  if (cachedNews && Date.now() - cachedNews.fetchedAt < NEWS_TTL_MS) {
    return cachedNews;
  }
  const item = await fetchLatestNews();
  cachedNews = { item, fetchedAt: Date.now() };
  return cachedNews;
}

export function invalidateNewsCache(): void {
  cachedNews = null;
}

export function getCache(): TierTableByCohort | null {
  return cachedTier;
}

export async function getHeroes(): Promise<Record<string, TencentHero>> {
  if (cachedHeroes) return cachedHeroes;
  cachedHeroes = await fetchHeroList();
  return cachedHeroes;
}

export async function refresh(): Promise<TierTableByCohort> {
  console.log("[refresh] fetching tencent…");
  cachedHeroes = await fetchHeroList();
  const table = await buildTierTable(krMap as Record<string, string>, cachedHeroes);
  cachedTier = table;
  const summary = Object.entries(table.cohorts)
    .map(([c, lanes]) => `${c}=${Object.values(lanes).reduce((a, b) => a + b.length, 0)}`)
    .join(" ");
  console.log(`[refresh] done. ${summary}`);
  return table;
}
