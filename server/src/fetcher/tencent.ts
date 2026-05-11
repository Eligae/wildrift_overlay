const TIER_URL =
  "https://mlol.qt.qq.com/go/lgame_battle_info/hero_rank_list_v2";
const HERO_LIST_URL =
  "https://game.gtimg.cn/images/lgamem/act/lrlib/js/heroList/hero_list.js";

export interface TencentTierRow {
  hero_id: string;
  position: string;
  appear_rate: string;
  forbid_rate: string;
  win_rate: string;
  strength: string;
  strength_level: string;
}

export interface TencentHero {
  heroId: string;
  name: string;
  title: string;
  alias: string;
  avatar: string;
  lane: string;
  roles: string[];
}

export interface NormalizedHero {
  heroId: string;
  cnName: string;
  alias: string;
  krName?: string;
  avatar: string;
  winRate: number;
  pickRate: number;
  banRate: number;
  strength: number;
  tierLevel: number;
}

export interface TierTable {
  fetchedAt: number;
  lanes: Record<LaneKey, NormalizedHero[]>;
}

export type LaneKey = "TOP" | "JUG" | "MID" | "ADC" | "SUP";

// 실측 검증된 매핑 (텐센트 hero_rank_list_v2).
const POSITION_TO_LANE: Record<string, LaneKey> = {
  "1": "MID",
  "2": "TOP",
  "3": "ADC",
  "4": "SUP",
  "5": "JUG",
};

export const LANE_ORDER: LaneKey[] = ["TOP", "JUG", "MID", "ADC", "SUP"];

interface TencentTierResponse {
  result: number;
  data: Record<string, Record<string, TencentTierRow[]>>;
}

interface TencentHeroListResponse {
  heroList: Record<string, TencentHero>;
}

export async function fetchTierData(): Promise<TencentTierResponse> {
  const res = await fetch(TIER_URL, {
    headers: { Referer: "https://lolm.qq.com/" },
  });
  if (!res.ok) throw new Error(`tier fetch failed: ${res.status}`);
  return (await res.json()) as TencentTierResponse;
}

export async function fetchHeroList(): Promise<Record<string, TencentHero>> {
  const res = await fetch(HERO_LIST_URL);
  if (!res.ok) throw new Error(`hero list fetch failed: ${res.status}`);
  const json = (await res.json()) as TencentHeroListResponse;
  return json.heroList;
}

export async function buildTierTable(
  krMap: Record<string, string>,
  mode: string = "0",
): Promise<TierTable> {
  const [tierResp, heroList] = await Promise.all([
    fetchTierData(),
    fetchHeroList(),
  ]);
  const modeData = tierResp.data[mode];
  if (!modeData) throw new Error(`unknown mode: ${mode}`);

  const lanes = {} as Record<LaneKey, NormalizedHero[]>;
  for (const [pos, laneKey] of Object.entries(POSITION_TO_LANE)) {
    const rows = modeData[pos] ?? [];
    const normalized: NormalizedHero[] = rows.map((row) => {
      const hero = heroList[row.hero_id];
      return {
        heroId: row.hero_id,
        cnName: hero?.name ?? "?",
        alias: hero?.alias ?? "",
        krName: krMap[row.hero_id],
        avatar: hero?.avatar ?? "",
        winRate: parseFloat(row.win_rate),
        pickRate: parseFloat(row.appear_rate),
        banRate: parseFloat(row.forbid_rate),
        strength: parseFloat(row.strength),
        tierLevel: parseInt(row.strength_level, 10),
      };
    });
    normalized.sort((a, b) => b.winRate - a.winRate);
    lanes[laneKey] = normalized;
  }

  return {
    fetchedAt: Date.now(),
    lanes,
  };
}
