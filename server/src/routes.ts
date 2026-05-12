import { Router } from "express";
import { getCache, refresh } from "./cache.js";
import { LANE_ORDER, LaneKey } from "./fetcher/tencent.js";
import { suggestSynergy, suggestCounter } from "./composition.js";

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

router.get("/composition/synergy", (req, res) => {
  const team = String(req.query.team ?? "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  if (team.length === 0) {
    res.status(400).json({ error: "team query required (comma-separated heroIds)" });
    return;
  }
  res.json({ team, suggestions: suggestSynergy(team) });
});

router.get("/composition/counter", (req, res) => {
  const enemy = String(req.query.enemy ?? "").trim();
  if (!enemy) {
    res.status(400).json({ error: "enemy query required" });
    return;
  }
  res.json(suggestCounter(enemy));
});

router.post("/refresh", async (_req, res) => {
  try {
    await refresh();
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: String(e instanceof Error ? e.message : e) });
  }
});
