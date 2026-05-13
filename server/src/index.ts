import express from "express";
import compression from "compression";
import cron from "node-cron";
import { router } from "./routes.js";
import { invalidateNewsCache, refresh } from "./cache.js";

const app = express();
const port = Number(process.env.PORT ?? 3000);

app.use(compression());

app.get("/", (_req, res) => {
  res.json({ ok: true, service: "wr-spellcheck-server" });
});
app.use("/v1", router);

async function main() {
  try {
    await refresh();
  } catch (e) {
    console.error("[init] refresh failed", e);
  }

  // 매일 03:00 UTC (KST 12:00) 갱신 — 티어/뉴스 캐시 모두 초기화
  cron.schedule("0 3 * * *", async () => {
    try {
      await refresh();
      invalidateNewsCache();
    } catch (e) {
      console.error("[cron] refresh failed", e);
    }
  });

  app.listen(port, () => {
    console.log(`[wr-server] listening on :${port}`);
  });
}

main();
