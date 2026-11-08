import os

p = "app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorColorMixerPanel.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

old_slider_block = """            // --- Single Active Slider (Zero Scroll) ---
            when (activeProperty) {
                0 -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_hue),
                    value = getCurrentHue(),
                    onValueChange = { updateCurrentHue(it) },
                    min = -100.0f,
                    max = 100.0f
                )
                1 -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_sat),
                    value = getCurrentSat(),
                    onValueChange = { updateCurrentSat(it) },
                    min = -100.0f,
                    max = 100.0f
                )
                else -> SzlBipolarSlider(
                    title = stringResource(R.string.tab_lum),
                    value = getCurrentLum(),
                    onValueChange = { updateCurrentLum(it) },
                    min = -100.0f,
                    max = 100.0f
                )
            }"""

new_slider_block = """            // --- 3 Sliders Visible Simultaneously matching Figma Node 12:931 ---
            val colorName = stringResource(spectralColors[selectedColorIndex].second)
            SzlBipolarSlider(
                title = "Teinte ($colorName)",
                value = getCurrentHue(),
                onValueChange = { updateCurrentHue(it) },
                min = -100.0f,
                max = 100.0f
            )
            SzlBipolarSlider(
                title = stringResource(R.string.tab_sat),
                value = getCurrentSat(),
                onValueChange = { updateCurrentSat(it) },
                min = -100.0f,
                max = 100.0f
            )
            SzlBipolarSlider(
                title = stringResource(R.string.tab_lum),
                value = getCurrentLum(),
                onValueChange = { updateCurrentLum(it) },
                min = -100.0f,
                max = 100.0f
            )"""

c = c.replace(old_slider_block, new_slider_block)

with open(p, "w", encoding="utf-8") as f:
    f.write(c)
print("Updated EditorColorMixerPanel.kt to show all 3 sliders simultaneously")
