with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioComponents.kt', 'rb') as f:
    data = f.read().decode('utf-8')

old_banner = """fun SectionTitleBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
        border = BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}"""

new_banner = """fun SectionTitleBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}"""

if old_banner in data:
    data = data.replace(old_banner, new_banner)
    with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioComponents.kt', 'wb') as f:
        f.write(data.encode('utf-8'))
    print('Updated StudioComponents successfully')
else:
    print('Target banner not found, trying normalized')
    # normalize whitespace
    import re
    data = re.sub(r'fun SectionTitleBanner\(.*?}\s*}\s*}', new_banner, data, flags=re.DOTALL)
    with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioComponents.kt', 'wb') as f:
        f.write(data.encode('utf-8'))
    print('Updated via regex')
