import os

p = "app/src/main/java/com/stealzalut/mobile/ui/screens/ProfileScreen.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

# Add SectionTitleBanner above PrimaryTabRow
old_tabs_start = """        // --- Primary Tabs (Photos vs Styles / Presets) ---
        PrimaryTabRow("""

new_tabs_start = """        // Title Banner matching Figma Node 12:1757 / 12:1813
        val profTitle = if (uiState.profileSelectedTab == 0) 
            stringResource(R.string.profile_title) 
        else 
            "Styles & Profils"
        val profSubtitle = if (uiState.profileSelectedTab == 0) 
            stringResource(R.string.profile_subtitle) 
        else 
            "Presets personnels et fichiers 3D LUT sauvegardés"

        com.stealzalut.mobile.ui.screens.studio.SectionTitleBanner(
            title = profTitle,
            subtitle = profSubtitle
        )

        // --- Primary Tabs (Photos vs Styles / Presets) ---
        PrimaryTabRow("""

c = c.replace(old_tabs_start, new_tabs_start)

# In tab 0, switch from 3 columns to 2 columns matching Figma
c = c.replace("columns = GridCells.Fixed(3),", "columns = GridCells.Fixed(2),")

with open(p, "w", encoding="utf-8") as f:
    f.write(c)
print("Updated ProfileScreen.kt")
