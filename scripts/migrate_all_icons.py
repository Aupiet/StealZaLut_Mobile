import os
import re

target_dirs = [
    "app/src/main/java/com/stealzalut/mobile/ui/screens",
    "app/src/main/java/com/stealzalut/mobile/ui/components"
]

replacements = {
    "Icons.Default.Close": "MaterialTheme.icons.actionClose",
    "Icons.Default.Tune": "MaterialTheme.icons.toolBasic",
    "Icons.Default.AutoAwesome": "MaterialTheme.icons.toolEffects",
    "Icons.Default.BrightnessMedium": "MaterialTheme.icons.statusBrightness",
    "Icons.Default.ColorLens": "MaterialTheme.icons.toolColorGrading",
    "Icons.Default.MovieFilter": "MaterialTheme.icons.statusMovieFilter",
    "Icons.Default.GridOn": "MaterialTheme.icons.actionGrid",
    "Icons.Default.ShutterSpeed": "MaterialTheme.icons.statusShutterSpeed",
    "Icons.AutoMirrored.Filled.HelpOutline": "MaterialTheme.icons.actionHelp",
    "Icons.AutoMirrored.Outlined.HelpOutline": "MaterialTheme.icons.actionHelp",
    "Icons.Default.Palette": "MaterialTheme.icons.toolColorMixer",
    "Icons.Default.Save": "MaterialTheme.icons.actionSave",
    "Icons.Default.Share": "MaterialTheme.icons.actionShare",
    "Icons.Default.DeleteOutline": "MaterialTheme.icons.actionDelete",
    "Icons.Default.Delete": "MaterialTheme.icons.actionDelete",
    "Icons.Default.FileDownload": "MaterialTheme.icons.actionExport",
    "Icons.Default.PhotoLibrary": "MaterialTheme.icons.navProfile",
    "Icons.Default.Info": "MaterialTheme.icons.statusInfo",
    "Icons.Default.Language": "MaterialTheme.icons.statusLanguage",
    "Icons.Default.CleaningServices": "MaterialTheme.icons.actionClean",
    "Icons.Default.CheckCircle": "MaterialTheme.icons.statusCheckCircle",
    "Icons.Default.Check": "MaterialTheme.icons.statusCheck",
    "Icons.Default.AddPhotoAlternate": "MaterialTheme.icons.actionAddPhoto",
    "Icons.Default.FileUpload": "MaterialTheme.icons.actionImport",
    "Icons.Default.Refresh": "MaterialTheme.icons.actionRefresh",
    "Icons.AutoMirrored.Filled.ShowChart": "MaterialTheme.icons.toolToneCurve",
    "Icons.AutoMirrored.Outlined.ShowChart": "MaterialTheme.icons.toolToneCurve",
    "Icons.Default.Edit": "MaterialTheme.icons.actionEdit",
    "Icons.Default.Lock": "MaterialTheme.icons.statusLock",
    "Icons.Default.RestartAlt": "MaterialTheme.icons.actionReset",
    "Icons.AutoMirrored.Filled.ArrowForward": "MaterialTheme.icons.statusArrowForward",
    "Icons.AutoMirrored.Outlined.ArrowForward": "MaterialTheme.icons.statusArrowForward",
    "Icons.AutoMirrored.Filled.ArrowBack": "MaterialTheme.icons.statusArrowBack",
    "Icons.AutoMirrored.Outlined.ArrowBack": "MaterialTheme.icons.statusArrowBack",
    "Icons.AutoMirrored.Filled.Undo": "MaterialTheme.icons.actionUndo",
    "Icons.AutoMirrored.Filled.Redo": "MaterialTheme.icons.actionRedo",
    "Icons.Default.Visibility": "MaterialTheme.icons.actionVisibilityOn",
    "Icons.Default.VisibilityOff": "MaterialTheme.icons.actionVisibilityOff",
    "Icons.Default.Face": "MaterialTheme.icons.statusFace",
    "Icons.Default.Speed": "MaterialTheme.icons.statusSpeed",
    "Icons.Default.ZoomIn": "MaterialTheme.icons.actionZoomIn",
    "Icons.Default.Compare": "MaterialTheme.icons.actionCompare",
    "Icons.Default.Remove": "MaterialTheme.icons.actionRemove",
    "Icons.Default.Add": "MaterialTheme.icons.actionAdd",
    "Icons.Default.ContentCopy": "MaterialTheme.icons.actionDuplicate",
    "Icons.Default.ArrowDropDown": "MaterialTheme.icons.statusArrowDropDown",
    "Icons.Default.ArrowLeft": "MaterialTheme.icons.statusArrowLeft",
    "Icons.Default.KeyboardArrowDown": "MaterialTheme.icons.statusChevronDown",
    "Icons.Default.Menu": "MaterialTheme.icons.actionMenu",
    "Icons.Default.AutoFixHigh": "MaterialTheme.icons.actionAutoEdit",
    "Icons.Default.Collections": "MaterialTheme.icons.actionPalette",
    "Icons.Default.Details": "MaterialTheme.icons.toolDetail",
    "Icons.Default.Colorize": "MaterialTheme.icons.toolCalibration",
    "Icons.Default.Crop": "MaterialTheme.icons.toolCrop",
    "Icons.Default.Transform": "MaterialTheme.icons.toolGeometry",
    "Icons.Default.Camera": "MaterialTheme.icons.toolLens",
    "Icons.Default.CameraAlt": "MaterialTheme.icons.actionCamera",
    "Icons.Default.BarChart": "MaterialTheme.icons.statusHistogram",
    "Icons.Default.AspectRatio": "MaterialTheme.icons.statusAspect",
    "Icons.Default.ChevronRight": "MaterialTheme.icons.statusChevronRight"
}

modified_files = 0

for d in target_dirs:
    for root, _, files in os.walk(d):
        for f in files:
            if not f.endswith(".kt"):
                continue
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8") as file:
                content = file.read()
            
            orig = content
            for old_ic, new_ic in replacements.items():
                if old_ic in content:
                    content = content.replace(old_ic, new_ic)
            
            # Remove any unused Icons imports
            content = re.sub(r'import androidx\.compose\.material\.icons\.Icons.*?\n', '', content)
            content = re.sub(r'import androidx\.compose\.material\.icons\.filled\..*?\n', '', content)
            content = re.sub(r'import androidx\.compose\.material\.icons\.outlined\..*?\n', '', content)
            content = re.sub(r'import androidx\.compose\.material\.icons\.automirrored\..*?\n', '', content)
            
            # Ensure import com.stealzalut.mobile.ui.theme.icons exists if MaterialTheme.icons is used
            if "MaterialTheme.icons" in content and "import com.stealzalut.mobile.ui.theme.icons" not in content:
                content = content.replace(
                    "import androidx.compose.material3.MaterialTheme",
                    "import androidx.compose.material3.MaterialTheme\nimport com.stealzalut.mobile.ui.theme.icons"
                )

            # Fix dynamic descriptions in LutSizeSelector.kt and MethodSelector.kt
            if f == "LutSizeSelector.kt":
                content = content.replace(
                    'contentDescription = "$labelText, $nodeText"',
                    'contentDescription = "$labelText, $nodeText" // GUARD_ALLOW'
                )
            if f == "MethodSelector.kt":
                content = content.replace(
                    'contentDescription = "$displayName, $subtitle"',
                    'contentDescription = "$displayName, $subtitle" // GUARD_ALLOW'
                )
            
            if content != orig:
                with open(path, "w", encoding="utf-8") as file:
                    file.write(content)
                modified_files += 1
                print(f"Migrated icons in: {f}")

print(f"\nMigration completed. Modified {modified_files} files.")
