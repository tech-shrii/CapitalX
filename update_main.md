Let me research exactly why Playwright treats this specific Stooq URL as a download.Now I have the exact confirmed pattern. Let me trace the precise root cause before writing:Two snippets only. Nothing else in the file changes.

---

### Snippet A — `_async_launch`: add `accept_downloads=True` to the context

Stooq sends `Content-Disposition: attachment` on the CSV URL. Without this flag Playwright has no mechanism to catch the download and just throws.

```python
# ─── FIND ────────────────────────────────────────────────────────────────────

        self._context = await self._browser.new_context(
            viewport={"width": 1280, "height": 800},
        )
```

```python
# ─── REPLACE WITH ────────────────────────────────────────────────────────────

        self._context = await self._browser.new_context(
            viewport={"width": 1280, "height": 800},
            accept_downloads=True,                  # Stooq CSV comes as an attachment
        )
```

---

### Snippet B — `_async_fetch`: swap `goto` + `expect_response` → `expect_download` + `goto`

This is the real fix. Stooq's server declares the CSV as a download (`Content-Disposition: attachment`), so Playwright aborts `goto()` before the navigation even commits — `expect_response` never fires. `expect_download` is the only API that catches this. The `Download` object gives us the text body directly via `read_text()`.

```python
# ─── FIND ────────────────────────────────────────────────────────────────────

    async def _async_fetch(self, url: str) -> str:
        """Navigate to *url*, capture the raw HTTP response body."""
        async with self._page.expect_response(
            lambda resp: resp.url == url and resp.status == 200
        ) as resp_info:
            await self._page.goto(url, wait_until="networkidle")

        response = await resp_info.value.text()
        await asyncio.sleep(_MIN_REQUEST_INTERVAL)   # honour rate limit
        return response
```

```python
# ─── REPLACE WITH ────────────────────────────────────────────────────────────

    async def _async_fetch(self, url: str) -> str:
        """
        Fetch *url* and return the body as a string.

        Stooq's /q/d/l/ endpoint returns
            Content-Disposition: attachment; filename="…"
        which makes Chromium treat it as a file download, not a page
        navigation.  page.goto() aborts immediately with "Download is
        starting" in that case.

        expect_download() is the correct Playwright API for this:
        it captures the Download object, and read_text() gives us the
        CSV body without ever writing to disk.
        """
        async with self._page.expect_download() as download_info:
            # goto() will "fail" (not commit the navigation) — that is
            # expected and fine; expect_download() catches the download
            # event that fires instead.
            await self._page.goto(url)

        download = await download_info.value
        text = await download.read_text()          # CSV body as str, no disk I/O
        await download.delete()                    # clean up the temp handle

        await asyncio.sleep(_MIN_REQUEST_INTERVAL) # honour Stooq rate limit
        return text
```

---

That's it — two surgical edits. The root cause: Stooq's CSV endpoint sends `Content-Disposition: attachment`, which is the server's way of saying *"save this file, don't render it"*. Chromium obeys that instruction, so `goto()` never finishes navigating and `expect_response` never fires. `expect_download` is Playwright's dedicated API for exactly this scenario — it intercepts the download event, and `read_text()` pulls the body straight out of memory with no temp files involved