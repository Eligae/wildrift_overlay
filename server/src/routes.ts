import { Router } from "express";
import { getCache, getHeroes, refresh } from "./cache.js";
import { LANE_ORDER, LaneKey } from "./fetcher/tencent.js";
import { suggestSynergy, suggestCounter } from "./composition.js";
import krMap from "../data/champion-kr.json" with { type: "json" };

export const router = Router();

router.get("/lanes", (_req, res) => {
  res.json({ lanes: LANE_ORDER });
});

router.get("/tier", (req, res) => {
  const cache = getCache();
  if (!cache) {
    res.status(503).json({ error: "no data yet" });
    return;
  }
  const laneParam = typeof req.query.lane === "string" ? req.query.lane.toUpperCase() : null;
  if (laneParam && (LANE_ORDER as string[]).includes(laneParam)) {
    const key = laneParam as LaneKey;
    res.json({
      fetchedAt: cache.fetchedAt,
      lane: key,
      champions: cache.lanes[key],
    });
    return;
  }
  res.json({
    fetchedAt: cache.fetchedAt,
    lanes: cache.lanes,
  });
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
    res.json({ champions: list });
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
  try {
    const heroes = await getHeroes();
    res.json({ team, suggestions: suggestSynergy(team, heroes) });
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

router.post("/refresh", async (_req, res) => {
  try {
    await refresh();
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: String(e instanceof Error ? e.message : e) });
  }
});
