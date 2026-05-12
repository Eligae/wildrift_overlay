import type { Request, Response, NextFunction } from "express";

const TOKEN = process.env.WR_API_TOKEN;

/**
 * API 키 미들웨어.
 * - `WR_API_TOKEN` 환경변수가 설정되어 있으면 헤더 `X-API-Key` 또는
 *   `Authorization: Bearer <token>` 검증.
 * - 미설정 시 로컬 개발 모드로 간주 — 검증 생략.
 */
export function authMiddleware(req: Request, res: Response, next: NextFunction) {
  if (!TOKEN) {
    next();
    return;
  }
  const headerKey = req.headers["x-api-key"];
  const auth = req.headers["authorization"];
  const provided =
    (typeof headerKey === "string" ? headerKey : null) ??
    (typeof auth === "string" ? auth.replace(/^Bearer\s+/i, "").trim() : null);
  if (provided && provided === TOKEN) {
    next();
  } else {
    res.status(401).json({ error: "unauthorized" });
  }
}
