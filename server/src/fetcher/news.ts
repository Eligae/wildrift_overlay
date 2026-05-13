const NEWS_URL = "https://wildrift.leagueoflegends.com/ko-kr/news/game-updates/";

export interface NewsItem {
  title: string;
  url: string; // 절대 URL
  publishedAt: string; // ISO
}

interface NextDataItem {
  title?: string;
  publishedAt?: string;
  category?: { machineName?: string };
  action?: { type?: string; payload?: { url?: string; youtubeId?: string } };
}

interface NextDataBlade {
  items?: NextDataItem[];
}

interface NextData {
  props?: {
    pageProps?: {
      page?: {
        blades?: NextDataBlade[];
      };
    };
  };
}

function toAbsoluteUrl(rel: string): string {
  if (/^https?:\/\//i.test(rel)) return rel;
  if (rel.startsWith("/")) return `https://wildrift.leagueoflegends.com${rel}`;
  return rel;
}

export async function fetchLatestNews(): Promise<NewsItem> {
  const res = await fetch(NEWS_URL, {
    headers: { "User-Agent": "Mozilla/5.0 (compatible; wr-spellcheck/1.0)" },
  });
  if (!res.ok) throw new Error(`news fetch failed: ${res.status}`);
  const html = await res.text();
  const m = html.match(/<script[^>]*id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
  if (!m) throw new Error("news: __NEXT_DATA__ not found");
  const data = JSON.parse(m[1]) as NextData;
  const blades = data.props?.pageProps?.page?.blades ?? [];
  // category.machineName === 'game_updates'인 첫 item.
  for (const blade of blades) {
    for (const item of blade.items ?? []) {
      if (item.category?.machineName !== "game_updates") continue;
      const title = item.title ?? "";
      const publishedAt = item.publishedAt ?? "";
      const rawUrl = item.action?.payload?.url ?? "";
      if (!title || !rawUrl) continue;
      return { title, url: toAbsoluteUrl(rawUrl), publishedAt };
    }
  }
  throw new Error("news: no game_updates item found");
}
