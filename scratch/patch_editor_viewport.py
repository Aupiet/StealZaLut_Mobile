import os

p = "app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorViewport.kt"
with open(p, "r", encoding="utf-8") as f:
    c = f.read()

# 1. Align histogram to TopStart (Figma Node 12:502 parity)
old_hist = """        // --- 5. Compact Floating Animated Glassmorphism RGB Histogram ---
        AnimatedVisibility(
            visible = isHistogramExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppDimens.SpacingMedium)
        )"""

new_hist = """        // --- 5. Compact Floating Animated Glassmorphism RGB Histogram (TopStart parity) ---
        AnimatedVisibility(
            visible = isHistogramExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = AppDimens.SpacingNormal, top = AppDimens.TopAppBarHeight + AppDimens.SpacingSmall)
        )"""

c = c.replace(old_hist, new_hist)

# 2. Add rounded corners to photo canvas
old_img = """        // --- 1. Photo Canvas ---
        displayBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )
        }"""

new_img = """        // --- 1. Photo Canvas with Darkroom Rounded Frame (Figma parity) ---
        displayBitmap?.let { bmp ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }
        }"""

c = c.replace(old_img, new_img)

with open(p, "w", encoding="utf-8") as f:
    f.write(c)
print("Updated EditorViewport.kt with rounded frame and TopStart histogram")
