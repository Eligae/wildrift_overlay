import express from "express";

const app = express();
const port = Number(process.env.PORT ?? 3000);

app.get("/", (_req, res) => {
  res.json({ ok: true, service: "wr-spellcheck-server" });
});

app.listen(port, () => {
  console.log(`[wr-server] listening on :${port}`);
});
