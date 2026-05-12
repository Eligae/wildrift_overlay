import { TencentHero, TierTable, buildTierTable, fetchHeroList } from "./fetcher/tencent.js";
import krMap from "../data/champion-kr.json" with { type: "json" };

let cachedTier: TierTable | null = null;
let cachedHeroes: Record<string, TencentHero> | null = null;

export function getCache(): TierTable | null {
  return cachedTier;
}

export async function getHeroes(): Promise<Record<string, TencentHero>> {
  if (cachedHeroes) return cachedHeroes;
  cachedHeroes = await fetchHeroList();
  return cachedHeroes;
}

export async function refresh(): Promise<TierTable> {
  console.log("[refresh] fetching tencent…");
  cachedHeroes = await fetchHeroList();
  const table = await buildTierTable(krMap as Record<string, string>, cachedHeroes);
  cachedTier = table;
  const counts = Object.entries(table.lanes)
    .map(([k, v]) => `${k}=${v.length}`)
    .join(" ");
  console.log(`[refresh] done. lanes: ${counts}`);
  return table;
}
