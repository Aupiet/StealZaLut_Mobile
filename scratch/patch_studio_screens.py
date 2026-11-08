import os

# 1. ImagePickerCard.kt: Remove duplicate floating button inside image preview
p_card = "app/src/main/java/com/stealzalut/mobile/ui/components/ImagePickerCard.kt"
with open(p_card, "r", encoding="utf-8") as f:
    c_card = f.read()

old_floating_btn = """                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(AppDimens.SpacingSmall),
                        shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = AppConstants.OPACITY_CONTAINER_HEAVY)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingExtraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MaterialTheme.icons.actionRefresh,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimens.IconMicro),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                            Text(
                                text = stringResource(R.string.wizard_step_1_change),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }"""

if old_floating_btn in c_card:
    c_card = c_card.replace(old_floating_btn, "")
    with open(p_card, "w", encoding="utf-8") as f:
        f.write(c_card)
    print("Patched ImagePickerCard.kt (removed duplicate floating button)")

# 2. StudioStep1Source.kt: Remove SectionTitleBanner (now in ApplyThemeScreen), refine layout
p_step1 = "app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep1Source.kt"
with open(p_step1, "r", encoding="utf-8") as f:
    c_step1 = f.read()

# Remove SectionTitleBanner call
old_banner_call = """        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_1_title),
            subtitle = stringResource(R.string.wizard_step_1_subtitle)
        )"""
if old_banner_call in c_step1:
    c_step1 = c_step1.replace(old_banner_call, "")

# Ensure mono font for EXIF pills
c_step1 = c_step1.replace(
    'style = MaterialTheme.typography.labelSmall,',
    'style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.stealzalut.mobile.ui.theme.LocalFontSpec.current.mono),'
)

with open(p_step1, "w", encoding="utf-8") as f:
    f.write(c_step1)
print("Patched StudioStep1Source.kt")

# 3. StudioStep2Style.kt: Remove SectionTitleBanner
p_step2 = "app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep2Style.kt"
with open(p_step2, "r", encoding="utf-8") as f:
    c_step2 = f.read()

old_banner2 = """        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_2_title),
            subtitle = stringResource(R.string.wizard_step_2_subtitle)
        )"""
if old_banner2 in c_step2:
    c_step2 = c_step2.replace(old_banner2, "")
    with open(p_step2, "w", encoding="utf-8") as f:
        f.write(c_step2)
    print("Patched StudioStep2Style.kt")

# 4. StudioStep3Parameters.kt: Remove SectionTitleBanner
p_step3 = "app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep3Parameters.kt"
with open(p_step3, "r", encoding="utf-8") as f:
    c_step3 = f.read()

old_banner3 = """        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_3_title),
            subtitle = stringResource(R.string.wizard_step_3_subtitle)
        )"""
if old_banner3 in c_step3:
    c_step3 = c_step3.replace(old_banner3, "")
    with open(p_step3, "w", encoding="utf-8") as f:
        f.write(c_step3)
    print("Patched StudioStep3Parameters.kt")

# 5. ApplyThemeScreen.kt: Add SectionTitleBanner ABOVE StudioStepper
p_apply = "app/src/main/java/com/stealzalut/mobile/ui/screens/ApplyThemeScreen.kt"
with open(p_apply, "r", encoding="utf-8") as f:
    c_apply = f.read()

old_stepper_block = """        // --- 1. Stepper Progress Header ---
        StudioStepper(
            currentStep = uiState.wizardStep,
            onStepClick = { step ->
                if (step < uiState.wizardStep) {
                    viewModel.setWizardStep(step)
                }
            }
        )"""

new_stepper_block = """        // --- 1. Title Banner matching Figma (Above Stepper) ---
        val (screenTitle, screenSubtitle) = when (uiState.wizardStep) {
            com.stealzalut.mobile.core.AppConstants.STEP_1_SOURCE -> 
                stringResource(R.string.wizard_step_1_title) to stringResource(R.string.wizard_step_1_subtitle)
            com.stealzalut.mobile.core.AppConstants.STEP_2_STYLE -> 
                stringResource(R.string.wizard_step_2_title) to stringResource(R.string.wizard_step_2_subtitle)
            com.stealzalut.mobile.core.AppConstants.STEP_3_PARAMS -> 
                stringResource(R.string.wizard_step_3_title) to stringResource(R.string.wizard_step_3_subtitle)
            com.stealzalut.mobile.core.AppConstants.STEP_4_COMPUTING -> 
                stringResource(R.string.wizard_step_4_title) to stringResource(R.string.wizard_step_4_subtitle)
            com.stealzalut.mobile.core.AppConstants.STEP_5_RESULT -> 
                stringResource(R.string.wizard_step_5_title) to stringResource(R.string.wizard_step_5_subtitle)
            else -> "" to ""
        }

        com.stealzalut.mobile.ui.screens.studio.SectionTitleBanner(
            title = screenTitle,
            subtitle = screenSubtitle
        )

        // --- 2. Stepper Progress Header ---
        StudioStepper(
            currentStep = uiState.wizardStep,
            onStepClick = { step ->
                if (step < uiState.wizardStep) {
                    viewModel.setWizardStep(step)
                }
            }
        )"""

c_apply = c_apply.replace(old_stepper_block, new_stepper_block)
with open(p_apply, "w", encoding="utf-8") as f:
    f.write(c_apply)
print("Patched ApplyThemeScreen.kt")
