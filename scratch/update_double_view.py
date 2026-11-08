import os

path = "app/src/main/java/com/stealzalut/mobile/ui/components/DoubleViewComparison.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

old_badges = """                    // Labels
                    SzlImageBadge(
                        text = stringResource(R.string.comparison_original_label),
                        alignment = Alignment.BottomStart,
                        isCompareMode = false
                    )
                    SzlImageBadge(
                        text = stringResource(R.string.comparison_lut_label),
                        alignment = Alignment.BottomEnd,
                        isCompareMode = false
                    )"""

new_badges = """                    // Badges matching Figma Node 12:405 / 12:407 (AVANT / APRÈS)
                    SzlImageBadge(
                        text = "AVANT",
                        alignment = Alignment.TopStart,
                        isCompareMode = false
                    )
                    SzlImageBadge(
                        text = "APRÈS",
                        alignment = Alignment.TopEnd,
                        isCompareMode = true
                    )"""

c = c.replace(old_badges, new_badges)
with open(path, "w", encoding="utf-8") as f:
    f.write(c)
print("Updated DoubleViewComparison.kt badges")
