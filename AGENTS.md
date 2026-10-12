# GAME REQUIREMENTS

@FEATURES.md

---

## /squideck-api

- **Architecture:** Clean Architecture or DDD or Suggests the most popular architecture and design patterns for Golang projects that widely used by pro go developers. Document in @FEATURES.md with the selected architecture and design patterns and explain why it is selected.
- **Database:** PostgreSQL
- **Tech Stack:** Golang

---

## /squideck-app

- **Tech Stack:** Kotlin, Jetpack Compose.
- **Colors:** Use only the Squid Game brand palette: Crimson `#AF0B45`, Pink `#F44786`, White `#FFFFFF`, Teal `#249F9C`, Dark Teal `#037A76`, Ink `#161313`. It is defined once in `ui/theme/Color.kt` (mirrored in `res/values/colors.xml`). Never hardcode a color or introduce a new one; lighter or darker variants must be derived from the palette (alpha, `tint`, `shade`). `BrandPaletteTest` enforces this.
- **Font:** The whole app uses the "Game Of Squids" font (`res/font/game_of_squids.ttf`, free fan font by Darrell Flood, licence in `assets/licenses/`), set on every style in `ui/theme/Type.kt`. Its lowercase letters are decorative, so all text is shown in capitals: use `GameText` (never Material `Text`) and `UppercaseTransformation` for text fields. `BrandPaletteTest` enforces this.

---

## Rules

- **Priority:** If you identify any conflicts between the requirements and the prompts, notify the user and ask for clarification. Update the requirements only after the user confirms how the conflicts should be resolved.
- Always use official docs, skills, mcp servers, search and widely used soluions etc. to get the correct information, don't assume anything because your trained data is old.
