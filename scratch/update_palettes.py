import os

p = "app/src/main/java/com/stealzalut/mobile/ui/screens/PalettesScreen.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

# Replace header surface with permanent SectionTitleBanner
old_header = """        // Section Header (Only shown when no photo is loaded)
        if (uiState.palettePhotoBitmap == null) {
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = MaterialTheme.icons.toolColorMixer,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconNormal),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = stringResource(R.string.palettes_header_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                    Text(
                        text = stringResource(R.string.palettes_header_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }"""

new_header = """        // Section Title Banner matching Figma Node 12:1691
        com.stealzalut.mobile.ui.screens.studio.SectionTitleBanner(
            title = stringResource(R.string.palettes_header_title),
            subtitle = stringResource(R.string.palettes_header_desc)
        )"""

c = c.replace(old_header, new_header)

with open(p, "w", encoding="utf-8") as f:
    f.write(c)
print("Updated PalettesScreen.kt header")
