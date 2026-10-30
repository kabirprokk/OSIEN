# KDS Studio — Minimalist Demo Website

Premium, minimalist portfolio for **KDS Studio (@kds__studio)** — wedding photography & filmmakers, founder @shamji__b_ahir.

## Run demo (2 sec)
1. Open `index.html` in browser, or:
```
cd kds-photography-demo
npx serve .
# or: python -m http.server 8000
```
2. Open http://localhost:8000

## What client sees
- Full-screen hero, Marquee, Philosophy
- Filterable Portfolio (All / Wedding / Pre-Wedding / Films)
- Lightbox gallery (click any image, arrows work)
- Services, Studio/About with stats, Testimonials
- Enquiry form → WhatsApp, Instagram CTA, mobile menu

## IMPORTANT: Real pics — DONE
Now using real KDS work from client Instagram save (`images/work-1..9, hero, studio` + `logo.webp`). © KDS Studio — for their site only.

## SPEED: will not lag — even with 50+ images/videos
- WebP with 480px thumb + 1000/1400px full (`-480.webp` ~30KB, `-1000.webp` ~150-340KB). Phone loads thumbs, desktop loads full only when visible.
- Hero preloaded (`fetchpriority=high`), everything else `loading=lazy + decoding=async + width/height` (no layout shift).
- Lightbox loads full only on click, preloads next/prev after open. Offscreen sections use `content-visibility:auto`.
- Initial load ≈ hero (340KB) + logo + CSS/JS ≈ under 500KB. Gallery streams as you scroll.
- To add more photos: export JPG 1600px, run `python optimize.py` pattern (1200px WebP q78 + 480px q68), reference `-480` in `src` + `-1000` in `data-full/srcset`.

## VIDEOS: no-lag rules (when you add films)
1. Compress: `ffmpeg -i in.mp4 -vcodec libx264 -crf 28 -preset veryfast -vf scale=1280:-2 -movflags +faststart -an out.mp4` + optional muted preview, poster = `-480.webp`.
2. Markup: `<video preload=none poster=... muted loop playsinline controls data-src=videos/film.mp4></video>` — JS in `script.js` loads only when visible, pauses offscreen.
3. Max 2-3 videos per page, never autoplay with sound, always poster. Reels: link to Instagram, don't self-host full reels.

## Next for final launch
- [ ] Replace WhatsApp number in `script.js` (WHATSAPP_NUMBER)
- [ ] Add real phone/email/city
- [ ] Connect form to email / Google Sheet if needed
- [ ] Buy domain + deploy (Vercel/Netlify free)
- [ ] Add credit line if any CC-BY assets remain (currently all CC0/Unsplash)

Built by AI Manager crew — library-first, CC0-first.
