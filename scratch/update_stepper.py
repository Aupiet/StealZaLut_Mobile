import os

path = "app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStepper.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

old_block = """                val targetCircleBg = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val targetContentColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                    isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }"""

new_block = """                val targetCircleBg = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val targetContentColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }"""

c = c.replace(old_block, new_block)

old_border = """                        .then(
                            if (isCurrent) {
                                Modifier.border(
                                    width = AppDimens.BorderMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )"""

new_border = """                        .then(
                            if (!isCurrent && !isCompleted) {
                                Modifier.border(
                                    width = AppDimens.BorderThin,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )"""

c = c.replace(old_border, new_border)

with open(path, "w", encoding="utf-8") as f:
    f.write(c)
print("Updated StudioStepper.kt")
