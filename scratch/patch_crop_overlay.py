import os

p = "app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorCropOverlay.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

# 1. Update CropRegion defaults to be visibly inset on the photo
old_region = """data class CropRegion(
    val left: Float = 0.0f,
    val top: Float = 0.0f,
    val right: Float = 1.0f,
    val bottom: Float = 1.0f,
    val angle: Float = 0.0f
)"""

new_region = """data class CropRegion(
    val left: Float = 0.08f,
    val top: Float = 0.08f,
    val right: Float = 0.92f,
    val bottom: Float = 0.92f,
    val angle: Float = 0.0f
)"""

c = c.replace(old_region, new_region)

# 2. Add horizontalScroll import if needed and make ratio pills row flexible
old_ratio_row = """        // Ratio Pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall, Alignment.CenterHorizontally)
        ) {
            CropAspectRatio.entries.forEach { ratio ->
                val isSelected = selectedRatio == ratio
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusFull),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(width = AppDimens.SegmentedButtonMinWidth, height = AppDimens.MinInteractiveTarget)
                        .clickable {"""

new_ratio_row = """        // Ratio Pills Row
        val ratioScrollState = androidx.compose.foundation.rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .androidx.compose.foundation.horizontalScroll(ratioScrollState),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall, Alignment.CenterHorizontally)
        ) {
            CropAspectRatio.entries.forEach { ratio ->
                val isSelected = selectedRatio == ratio
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusFull),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .androidx.compose.foundation.layout.defaultMinSize(minWidth = androidx.compose.ui.unit.dp(58), minHeight = AppDimens.MinInteractiveTarget)
                        .clickable {"""

c = c.replace(old_ratio_row, new_ratio_row)

c = c.replace(
    """                        Text(
                            text = stringResource(ratio.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )""",
    """                        Text(
                            text = stringResource(ratio.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall)
                        )"""
)

with open(p, "w", encoding="utf-8") as f:
    f.write(c)
print("Updated EditorCropOverlay.kt")
