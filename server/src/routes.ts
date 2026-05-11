import { Router } from "express";
import { getCache, refresh } from "./cache.js";
import { LANE_ORDER, LaneKey } from "./fetcher/tencent.js";

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

router.post("/refresh", async (_req, res) => {
  try {
    await refresh();
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: String(e instanceof Error ? e.message : e) });
  }
});
