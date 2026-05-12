import tags from "../data/champion-tags.json" with { type: "json" };
import counters from "../data/counter-matrix.json" with { type: "json" };
import krMap from "../data/champion-kr.json" with { type: "json" };

interface ChampionTags {
  class: string;
  roles: string[];
  cc: string;
  range: string;
  damage: string;
}

interface CounterEntry {
  counters: string[];
  note?: string;
}

const ALL_TAGS: Record<string, ChampionTags> = tags as Record<string, ChampionTags>;
const COUNTERS: Record<string, CounterEntry> = counters as Record<string, CounterEntry>;
const KR_MAP: Record<string, string> = krMap as Record<string, string>;

export interface SynergySuggestion {
  heroId: string;
  krName?: string;
  reasons: string[];
}

export function suggestSynergy(teamHeroIds: string[]): SynergySuggestion[] {
  const teamTags = teamHeroIds.map((id) => ALL_TAGS[id]).filter((t): t is ChampionTags => !!t);

  const hasTank = teamTags.some((t) => t.class === "tank" || t.class === "fighter");
  const hasHardCC = teamTags.some((t) => t.cc === "hard");
  const hasRangedCarry = teamTags.some(
    (t) => t.range === "ranged" && (t.class === "marksman" || t.class === "mage"),
  );
  const hasEngage = teamTags.some((t) => t.roles.includes("engage"));
  const adCount = teamTags.filter((t) => t.damage === "AD").length;
  const apCount = teamTags.filter((t) => t.damage === "AP").length;

  const needs = {
    tank: !hasTank,
    hardCC: !hasHardCC,
    ranged: !hasRangedCarry,
    engage: !hasEngage,
    needAP: adCount >= 4,
    needAD: apCount >= 4,
  };

  return Object.entries(ALL_TAGS)
    .filter(([id]) => !teamHeroIds.includes(id))
    .map(([id, t]) => {
      const reasons: string[] = [];
      if (needs.tank && (t.class === "tank" || t.class === "fighter"))
        reasons.push("팀에 탱커가 없습니다");
      if (needs.hardCC && t.cc === "hard") reasons.push("하드 CC가 부족합니다");
      if (needs.ranged && t.range === "ranged" && (t.class === "marksman" || t.class === "mage"))
        reasons.push("원거리 캐리가 없습니다");
      if (needs.engage && t.roles.includes("engage")) reasons.push("이니시에이터가 없습니다");
      if (needs.needAP && t.damage === "AP") reasons.push("AP 데미지가 부족합니다");
      if (needs.needAD && t.damage === "AD") reasons.push("AD 데미지가 부족합니다");
      return { heroId: id, krName: KR_MAP[id], reasons };
    })
    .filter((c) => c.reasons.length > 0)
    .sort((a, b) => b.reasons.length - a.reasons.length)
    .slice(0, 5);
}

export interface CounterSuggestion {
  enemyHeroId: string;
  enemyKrName?: string;
  counters: Array<{ heroId: string; krName?: string }>;
  note?: string;
}

export function suggestCounter(enemyHeroId: string): CounterSuggestion {
  const entry = COUNTERS[enemyHeroId];
  return {
    enemyHeroId,
    enemyKrName: KR_MAP[enemyHeroId],
    counters: (entry?.counters ?? []).map((id) => ({
      heroId: id,
      krName: KR_MAP[id],
    })),
    note: entry?.note,
  };
}
