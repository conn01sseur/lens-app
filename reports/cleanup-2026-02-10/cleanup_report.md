# Cleanup Report (2026-02-10)

## Scope
Static audit of project files for "likely unused" candidates.

Scanned roots:
- `app/src/main/java`
- `app/src/main/res`
- `app/src/main/assets`
- `app/src/main/AndroidManifest.xml`

## Totals
- repo files: `765`
- runtime source files in `app/src/main`: `292`
- Kotlin files: `29`
- Layout files: `8`
- Drawable files: `93`
- Assets files: `119`
- Raw audio files: `8`

## Key Findings
1. `layout` likely unused:
- `activity_splash` (not set via `setContentView` in `SplashActivity`)

2. `assets/pages` likely unused:
- `pages/image-14.png`

3. `drawable` likely unused: `38` files
- Full list: `drawable_unused.txt`
- Size-ranked list: `drawable_unused_sizes.txt`

4. `assets/resources` likely unused: `70` files
- Full list: `resource_assets_unused.txt`
- Top by size: `resource_unused_topsize.txt`

## High-Impact Size Candidates
From `assets/resources` candidates:
- `resources/Узоры.png` ~ 65,573,532 bytes
- `resources/pattern.png` ~ 65,573,532 bytes
- `resources/приветствие/фон.png` ~ 4,473,247 bytes
- `resources/О Ленских столбах.png` ~ 2,885,206 bytes
- `resources/знакомство/5.png` ~ 2,165,324 bytes
- `resources/Главная страница.png` ~ 1,317,516 bytes

These are likely mockups/source assets and should be the first cleanup target if you want APK/project size reduction.

## Safety Notes
- This is a static reference audit. Dynamic usage via reflection, string-constructed resource names, or future features may exist.
- Before deleting any candidate group:
  - delete in small batches,
  - run a full build,
  - smoke-test key flows (menu, map, weather, audio, downloads).

## Recommended Cleanup Order
1. Remove obvious dead pages/layouts:
- `activity_splash`
- `pages/image-14.png`

2. Remove heavy mockup assets under `assets/resources` (large files first).

3. Remove unused drawables from `drawable_unused.txt`.

4. Rebuild and test after each batch.

## About `.rd5` Download Behavior
`Скачивание маршрута` does **not** download all `.rd5`.
It calls `downloadLenskiePackage(...)` and downloads only the Lenskie area package:
- bounds: lat `59..63`, lon `124..130`
- grid: `2 x 3` => `6` `.rd5` files
- already existing files are skipped.

Separate flow `downloadYakutia(...)` (in `MainActivity`) downloads a much larger Yakutia set.

## Attached Lists
- `drawable_unused.txt`
- `drawable_unused_sizes.txt`
- `layout_unused.txt`
- `page_unused.txt`
- `resource_assets_unused.txt`
- `resource_unused_topsize.txt`
