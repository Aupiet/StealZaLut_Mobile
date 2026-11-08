with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStepper.kt', 'rb') as f:
    data = f.read().decode('utf-8')

old_code = """                val targetCircleBg = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val targetContentColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                    isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }"""

new_code = """                val targetCircleBg = when {
                    isCompleted -> MaterialTheme.szlColors.success
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val targetContentColor = when {
                    isCompleted -> MaterialTheme.szlColors.onSuccess
                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }"""

data = data.replace(old_code, new_code)

old_line = """                    val lineColor by animateColorAsState(
                        targetValue = if (step < currentStep) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE),
                        animationSpec = tween(durationMillis = 250),
                        label = "stepperLine" // GUARD_ALLOW
                    )"""

new_line = """                    val lineColor by animateColorAsState(
                        targetValue = when {
                            step < currentStep - 1 -> MaterialTheme.szlColors.success
                            step == currentStep - 1 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        },
                        animationSpec = tween(durationMillis = 250),
                        label = "stepperLine" // GUARD_ALLOW
                    )"""

data = data.replace(old_line, new_line)

with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStepper.kt', 'wb') as f:
    f.write(data.encode('utf-8'))
print('Updated StudioStepper successfully')
