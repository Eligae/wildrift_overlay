import { Router } from "express";
import { createHash } from "node:crypto";
import { getCache, getHeroes, getLatestNews, invalidateNewsCache, refresh } from "./cache.js";
import { COHORT_ORDER, Cohort, LANE_ORDER, LaneKey } from "./fetcher/tencent.js";
import { suggestSynergy, suggestCounter } from "./composition.js";
import { authMiddleware } from "./auth.js";
import krMap from "../data/champion-kr.json" with { type: "json" };
import skinsRaw from "../data/champion-skins.json" with { type: "json" };

// 시작 시 1회 계산. 데이터 변경 = 컨테이너 재배포 = 재계산.
// 챔피언별 sha를 따로 들고, 변경된 챔피언만 incremental 전송 가능하게.
interface ChampSkinEntry { krName: string; skins: string[]; sha256: string }
const SKINS_PER_CHAMP: Record<string, ChampSkinEntry> = (() => {
  const data = skinsRaw as {
    fetchedAt: string;
    champions: Record<string, { krName: string; skins: string[] }>;
  };
  const out: Record<string, ChampSkinEntry> = {};
  for (const [alias, c] of Object.entries(data.champions)) {
    const json = JSON.stringify({ krName: c.krName, skins: c.skins });
    out[alias] = {
      krName: c.krName,
      skins: c.skins,
      sha256: createHash("sha256").update(json).digest("hex"),
    };
  }
  return out;
})();
const SKINS_FETCHED_AT = (skinsRaw as { fetchedAt: string }).fetchedAt;
const SKINS_GLOBAL_SHA = createHash("sha256")
  .update(
    JSON.stringify(
      Object.fromEntries(Object.entries(SKINS_PER_CHAMP).map(([a, e]) => [a, e.sha256])),
    ),
  )
  .digest("hex");
const SKINS_MANIFEST = {
  fetchedAt: SKINS_FETCHED_AT,
  sha256: SKINS_GLOBAL_SHA,
  champions: Object.fromEntries(
    Object.entries(SKINS_PER_CHAMP).map(([a, e]) => [a, e.sha256]),
  ),
};
// 평탄화 풀 응답 (첫 로드 / fallback용). skin 이름 → krName.
const SKINS_FULL_MAP: Record<string, string> = (() => {
  const map: Record<string, string> = {};
  for (const e of Object.values(SKINS_PER_CHAMP)) for (const s of e.skins) map[s] = e.krName;
  return map;
})();

export const router = Router();

// 모든 /v1/* 요청에 토큰 검증 (WR_API_TOKEN 환경변수 설정 시).
router.use(authMiddleware);

router.get("/lanes", (_req, res) => {
  res.json({ lanes: LANE_ORDER });
});

router.get("/tier", (req, res) => {
  const cache = getCache();
  if (!cache) {
    res.status(503).json({ error: "no data yet" });
    return;
  }
  res.set("Cache-Control", "public, max-age=21600"); // 6시간
  const cohortParam = typeof req.query.cohort === "string" ? req.query.cohort.toUpperCase() : null;
  const cohort: Cohort = (COHORT_ORDER as string[]).includes(cohortParam ?? "")
    ? (cohortParam as Cohort)
    : "DIAMOND";
  const lanes = cache.cohorts[cohort];
  const laneParam = typeof req.query.lane === "string" ? req.query.lane.toUpperCase() : null;
  if (laneParam && (LANE_ORDER as string[]).includes(laneParam)) {
    const key = laneParam as LaneKey;
    res.json({
      fetchedAt: cache.fetchedAt,
      cohort,
      lane: key,
      champions: lanes[key],
    });
    return;
  }
  res.json({
    fetchedAt: cache.fetchedAt,
    cohort,
    lanes,
  });
});

router.get("/tier/all", (_req, res) => {
  const cache = getCache();
  if (!cache) {
    res.status(503).json({ error: "no data yet" });
    return;
  }
  res.set("Cache-Control", "public, max-age=21600");
  res.json({
    fetchedAt: cache.fetchedAt,
    cohorts: cache.cohorts,
  });
});

router.get("/champion-skins/version", (_req, res) => {
  // 본문 ~80B — 클라가 매번 폴링해도 부담 X.
  res.set("Cache-Control", "public, max-age=3600");
  res.json({ sha256: SKINS_GLOBAL_SHA, fetchedAt: SKINS_FETCHED_AT });
});

router.get("/champion-skins/manifest", (_req, res) => {
  // 챔피언별 sha 목록 — 클라가 로컬과 비교해서 변경된 챔피언만 골라낸다.
  res.set("Cache-Control", "public, max-age=3600");
  res.set("ETag", `"${SKINS_GLOBAL_SHA}"`);
  res.json(SKINS_MANIFEST);
});

router.get("/champion-skins/c/:alias", (req, res) => {
  const alias = String(req.params.alias);
  const entry = SKINS_PER_CHAMP[alias];
  if (!entry) {
    res.status(404).json({ error: "alias not found" });
    return;
  }
  res.set("Cache-Control", "public, max-age=86400");
  res.set("ETag", `"${entry.sha256}"`);
  res.json({ alias, krName: entry.krName, skins: entry.skins, sha256: entry.sha256 });
});

router.get("/champion-skins", (_req, res) => {
  // 풀 응답 — 첫 로드/폴백용. skin name → krName 평탄화 + global sha.
  res.set("Cache-Control", "public, max-age=86400");
  res.set("ETag", `"${SKINS_GLOBAL_SHA}"`);
  res.json({ fetchedAt: SKINS_FETCHED_AT, sha256: SKINS_GLOBAL_SHA, skins: SKINS_FULL_MAP });
});

router.get("/champions", async (_req, res) => {
  try {
    const heroes = await getHeroes();
    const list = Object.entries(krMap as Record<string, string>)
      .map(([heroId, krName]) => ({
        heroId,
        krName,
        avatar: heroes[heroId]?.avatar ?? "",
      }))
      .sort((a, b) => a.krName.localeCompare(b.krName, "ko"));
    res.set("Cache-Control", "public, max-age=43200"); // 12시간 (한국어명 거의 안 변함)
    res.json({ champions: list, fetchedAt: Date.now() });
  } catch (e) {
    res.status(500).json({ error: String(e instanceof Error ? e.message : e) });
  }
});

router.get("/composition/synergy", async (req, res) => {
  const team = String(req.query.team ?? "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  if (team.length === 0) {
    res.status(400).json({ error: "team query required (comma-separated heroIds)" });
    return;
  }
  const laneParam = typeof req.query.lane === "string" ? req.query.lane.toUpperCase() : null;
  let laneHeroIds: Set<string> | undefined;
  if (laneParam && (LANE_ORDER as string[]).includes(laneParam)) {
    const cache = getCache();
    if (cache) {
      // DIAMOND cohort 기준 — 가장 넓은 표본. 해당 lane에 데이터 있는 챔피언만 후보.
      laneHeroIds = new Set(cache.cohorts.DIAMOND[laneParam as LaneKey].map((c) => c.heroId));
    }
  }
  try {
    const heroes = await getHeroes();
    res.json({ team, lane: laneParam, suggestions: suggestSynergy(team, heroes, laneHeroIds) });
  } catch (e) {
    res.status(500).json({ error: String(e instanceof Error ? e.message : e) });
  }
});

router.get("/composition/counter", async (req, res) => {
  const enemy = String(req.query.enemy ?? "").trim();
  if (!enemy) {
    res.status(400).json({ error: "enemy query required" });
    return;
  }
  try {
    const heroes = await getHeroes();
    res.json(suggestCounter(enemy, heroes));
  } catch (e) {
    res.status(500).json({ error: String(e instanceof Error ? e.message : e) });
  }
});

router.get("/news/latest", async (_req, res) => {
  try {
    const { item, fetchedAt } = await getLatestNews();
    res.set("Cache-Control", "public, max-age=86400"); // 24시간
    res.json({ ...item, fetchedAt });
  } catch (e) {
    res.status(502).json({ error: String(e instanceof Error ? e.message : e) });
  }
});

router.post("/refresh", async (_req, res) => {
  try {
    await refresh();
    invalidateNewsCache(); // 티어 갱신 시 뉴스 캐시도 무효화
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: String(e instanceof Error ? e.message : e) });
  }
});
