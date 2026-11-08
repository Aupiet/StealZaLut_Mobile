import os

# 1. SzlBipolarSlider.kt
p1 = r"app/src/main/java/com/stealzalut/mobile/ui/components/SzlBipolarSlider.kt"
with open(p1, "r", encoding="utf-8") as f:
    c1 = f.read()

c1 = c1.replace(
    "style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.stealzalut.mobile.ui.theme.SzlTheme.fonts.mono),",
    "style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.stealzalut.mobile.ui.theme.LocalFontSpec.current.mono),"
)
with open(p1, "w", encoding="utf-8") as f:
    f.write(c1)
print("Patched SzlBipolarSlider.kt")

# 2. SzlSlider.kt
p2 = r"app/src/main/java/com/stealzalut/mobile/ui/components/SzlSlider.kt"
with open(p2, "r", encoding="utf-8") as f:
    c2 = f.read()

# Add imports
if "import androidx.compose.foundation.background" not in c2:
    c2 = c2.replace(
        "import androidx.compose.foundation.Canvas",
        "import androidx.compose.foundation.Canvas\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.layout.Box"
    )

# Extract thumb colors before Slider
old_slider_start = """            Slider(
                value = value,"""
new_slider_start = """            val thumbSurfaceColor = MaterialTheme.colorScheme.surface
            val thumbPrimaryColor = MaterialTheme.colorScheme.primary

            Slider(
                value = value,"""
c2 = c2.replace(old_slider_start, new_slider_start)
c2 = c2.replace(".background(MaterialTheme.colorScheme.surface)", ".background(thumbSurfaceColor)")
c2 = c2.replace(".background(MaterialTheme.colorScheme.primary)", ".background(thumbPrimaryColor)")

with open(p2, "w", encoding="utf-8") as f:
    f.write(c2)
print("Patched SzlSlider.kt")

# 3. MainColorGradingScreen.kt
p3 = r"app/src/main/java/com/stealzalut/mobile/ui/screens/MainColorGradingScreen.kt"
with open(p3, "r", encoding="utf-8") as f:
    c3 = f.read()

if "import androidx.compose.foundation.layout.statusBarsPadding" not in c3:
    c3 = c3.replace(
        "import androidx.compose.foundation.layout.size",
        "import androidx.compose.foundation.layout.size\nimport androidx.compose.foundation.layout.statusBarsPadding\nimport androidx.compose.foundation.layout.defaultMinSize"
    )

with open(p3, "w", encoding="utf-8") as f:
    f.write(c3)
print("Patched MainColorGradingScreen.kt")

# 4. EditorEmptyState.kt
p4 = r"app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorEmptyState.kt"
with open(p4, "r", encoding="utf-8") as f:
    c4 = f.read()

if "import androidx.compose.foundation.layout.width" not in c4:
    c4 = c4.replace(
        "import androidx.compose.foundation.layout.size",
        "import androidx.compose.foundation.layout.size\nimport androidx.compose.foundation.layout.width"
    )

with open(p4, "w", encoding="utf-8") as f:
    f.write(c4)
print("Patched EditorEmptyState.kt")
