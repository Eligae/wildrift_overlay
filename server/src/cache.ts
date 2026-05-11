import { TierTable, buildTierTable } from "./fetcher/tencent.js";
import krMap from "../data/champion-kr.json" with { type: "json" };

let cached: TierTable | null = null;

export function getCache(): TierTable | null {
  return cached;
}

export async function refresh(): Promise<TierTable> {
  console.log("[refresh] fetching tencent…");
  const table = await buildTierTable(krMap as Record<string, string>);
  cached = table;
  const counts = Object.entries(table.lanes)
    .map(([k, v]) => `${k}=${v.length}`)
    .join(" ");
  console.log(`[refresh] done. lanes: ${counts}`);
  return table;
}
