# Pocketsum brand

**Name:** Pocketsum  **Developer:** Codigitech  **Tagline:** Simple, offline money tracking.
**Personality:** sharp, trustworthy, understated. Dark-mode first.

## Palette

| Token | Light | Dark | Used for |
|---|---|---|---|
| Charcoal (brand) | `#111614` | `#111614` | icon background, splash, hero (dark) |
| Emerald (primary) | `#059669` | `#34D399` | primary actions, income |
| Emerald mark | `#10B981` | `#10B981` | pocket in the logo, iOS accent |
| Mint | `#A7F3D0` | `#A7F3D0` | coin in the logo, muted text on hero |
| Surface | `#F7F8F7` | `#0B0F0E` | screen background |
| On surface | `#171D1A` | `#E6EAE7` | body text |
| Secondary (slate green) | `#4B5D57` | `#B0C0BA` | secondary text, chips |
| Tertiary (steel blue) | `#3B5FA0` | `#A3C1FF` | rarely, informational accents |
| Expense (coral) | `#DC2626` | `#F87171` | expenses, negative balances |
| Transfer (sky) | `#0284C7` | `#38BDF8` | transfers |

Source of truth in code: `composeApp/src/commonMain/kotlin/com/codigitech/ft/ui/theme/Theme.kt`,
`composeApp/src/androidMain/res/values/colors.xml`, `iosApp/iosApp/Assets.xcassets/*.colorset`.

## Logo

A mint coin with a "+" (the *sum*) peeking out of an emerald pocket, on charcoal. Flat, no gradients.
Master: `logo/pocketsum-icon.svg` (108-unit adaptive-icon grid, content inside the 66-unit safe circle).
Wordmark: `logo/pocketsum-wordmark.svg` ("Pocket" in off-white, "sum" in emerald `#34D399`, system sans SemiBold).

Rules
- Never recolour the mark. On light surfaces use the tile version (charcoal rounded square), not the bare glyph.
- Minimum size 24 px. The monochrome silhouette (`ic_launcher_monochrome.xml`) is for Android themed icons and the notification tray only.
- Keep at least 10% of the tile width as clear space around the wordmark.

## Regenerating assets

All PNGs are rendered from the same geometry by `logo/Render.java` (no external tools):

```
java docs/branding/logo/Render.java .
```

Outputs: Android legacy mipmaps (48–192 px, rounded and circle), iOS `AppIcon.png` 1024, iOS launch glyphs,
`store/icon-512.png`, `store/feature-graphic-1024x500.png`. Edit the geometry in `Render.java`,
`pocketsum-icon.svg` and `ic_launcher_foreground.xml` together.
