path = "app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep3Parameters.kt"
with open(path, "r", encoding="utf-8") as f:
    text = f.read()

target = """    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. PINNED LIVE WYSIWYG PREVIEW CARD ---"""

replacement = """    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_3_title),
            subtitle = stringResource(R.string.wizard_step_3_subtitle)
        )

        // --- 1. PINNED LIVE WYSIWYG PREVIEW CARD ---"""

if target in text:
    text = text.replace(target, replacement)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    print("Patched successfully!")
else:
    print("Target not found directly, checking normalized...")
    norm_text = text.replace("\r\n", "\n")
    norm_target = target.replace("\r\n", "\n")
    if norm_target in norm_text:
        norm_text = norm_text.replace(norm_target, replacement.replace("\r\n", "\n"))
        with open(path, "w", encoding="utf-8") as f:
            f.write(norm_text)
        print("Patched with normalized CRLF!")
    else:
        print("Not found in normalized either!")
