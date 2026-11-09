package com.stealzalut.mobile.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowLeft
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Details
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShutterSpeed
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowLeft
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Details
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShutterSpeed
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Semantic Icon Set abstraction for StealZaLUT Mobile.
 *
 * Screens MUST NEVER reference Icons.Default or third-party icons directly.
 * Instead, they access semantic tokens via `SzlTheme.icons.<token>` or `MaterialTheme.icons.<token>`.
 */
@Immutable
interface SzlIconSet {
    // Semantic Roles (Figma 1:1)
    val toolCrop: ImageVector
    val toolBasic: ImageVector
    val toolToneCurve: ImageVector
    val toolColorMixer: ImageVector
    val toolColorGrading: ImageVector
    val toolDetail: ImageVector
    val toolEffects: ImageVector
    val toolCalibration: ImageVector
    val toolGeometry: ImageVector
    val toolLens: ImageVector

    val export: ImageVector get() = actionExport
    val actionExport: ImageVector
    val actionImport: ImageVector
    val actionSave: ImageVector
    val undo: ImageVector get() = actionUndo
    val actionUndo: ImageVector
    val redo: ImageVector get() = actionRedo
    val actionRedo: ImageVector
    val compare: ImageVector get() = actionCompare
    val actionCompare: ImageVector
    val actionAutoEdit: ImageVector
    val actionReset: ImageVector
    val actionRefresh: ImageVector
    val menu: ImageVector get() = actionMenu
    val actionMenu: ImageVector
    val actionHelp: ImageVector
    val actionBack: ImageVector
    val actionSearch: ImageVector
    val actionChevronRight: ImageVector
    val delete: ImageVector get() = actionDelete
    val actionDelete: ImageVector
    val actionShare: ImageVector
    val actionVisibilityOn: ImageVector
    val actionVisibilityOff: ImageVector
    val actionClose: ImageVector
    val gallery: ImageVector get() = actionAddPhoto
    val actionAddPhoto: ImageVector
    val actionDuplicate: ImageVector
    val actionCamera: ImageVector
    val actionGrid: ImageVector
    val palette: ImageVector get() = actionPalette
    val actionPalette: ImageVector
    val actionAdd: ImageVector
    val actionRemove: ImageVector
    val actionEdit: ImageVector
    val actionClean: ImageVector
    val actionZoomIn: ImageVector

    // Navigation Destinations
    val studio: ImageVector get() = navStudio
    val navStudio: ImageVector
    val editor: ImageVector get() = navEditor
    val navEditor: ImageVector
    val navPalettes: ImageVector
    val profile: ImageVector get() = navProfile
    val navProfile: ImageVector
    val settings: ImageVector get() = navSettings
    val navSettings: ImageVector

    // Status & Utility Indicators
    val check: ImageVector get() = statusCheck
    val statusCheck: ImageVector
    val statusCheckCircle: ImageVector
    val info: ImageVector get() = statusInfo
    val statusInfo: ImageVector
    val statusChevronDown: ImageVector
    val statusChevronRight: ImageVector
    val statusArrowBack: ImageVector
    val next: ImageVector get() = statusArrowForward
    val statusArrowForward: ImageVector
    val statusArrowDropDown: ImageVector
    val statusArrowLeft: ImageVector
    val histogram: ImageVector get() = statusHistogram
    val statusHistogram: ImageVector
    val statusAspect: ImageVector
    val statusBrightness: ImageVector
    val statusMovieFilter: ImageVector
    val statusShutterSpeed: ImageVector
    val statusSpeed: ImageVector
    val statusFace: ImageVector
    val statusLock: ImageVector
    val statusLanguage: ImageVector
}

/**
 * Primary icon set: Material Rounded icons (Chic & playful aesthetic).
 */
object RoundedSzlIconSet : SzlIconSet {
    override val toolBasic: ImageVector = Icons.Rounded.Tune
    override val toolToneCurve: ImageVector = Icons.AutoMirrored.Rounded.ShowChart
    override val toolColorMixer: ImageVector = Icons.Rounded.Palette
    override val toolColorGrading: ImageVector = Icons.Rounded.ColorLens
    override val toolDetail: ImageVector = Icons.Rounded.Details
    override val toolEffects: ImageVector = Icons.Rounded.AutoAwesome
    override val toolCalibration: ImageVector = Icons.Rounded.Colorize
    override val toolCrop: ImageVector = Icons.Rounded.Crop
    override val toolGeometry: ImageVector = Icons.Rounded.Transform
    override val toolLens: ImageVector = Icons.Rounded.Camera

    override val actionExport: ImageVector = Icons.Rounded.FileDownload
    override val actionImport: ImageVector = Icons.Rounded.FileUpload
    override val actionSave: ImageVector = Icons.Rounded.Save
    override val actionUndo: ImageVector = Icons.AutoMirrored.Rounded.Undo
    override val actionRedo: ImageVector = Icons.AutoMirrored.Rounded.Redo
    override val actionCompare: ImageVector = Icons.Rounded.Compare
    override val actionAutoEdit: ImageVector = Icons.Rounded.AutoFixHigh
    override val actionReset: ImageVector = Icons.Rounded.RestartAlt
    override val actionRefresh: ImageVector = Icons.Rounded.Refresh
    override val actionMenu: ImageVector = Icons.Rounded.MoreVert
    override val actionHelp: ImageVector = Icons.AutoMirrored.Rounded.HelpOutline
    override val actionBack: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    override val actionSearch: ImageVector = Icons.Rounded.Search
    override val actionChevronRight: ImageVector = Icons.Rounded.ChevronRight
    override val actionDelete: ImageVector = Icons.Rounded.DeleteOutline
    override val actionShare: ImageVector = Icons.Rounded.Share
    override val actionVisibilityOn: ImageVector = Icons.Rounded.Visibility
    override val actionVisibilityOff: ImageVector = Icons.Rounded.VisibilityOff
    override val actionClose: ImageVector = Icons.Rounded.Close
    override val actionAddPhoto: ImageVector = Icons.Rounded.AddPhotoAlternate
    override val actionDuplicate: ImageVector = Icons.Rounded.ContentCopy
    override val actionCamera: ImageVector = Icons.Rounded.CameraAlt
    override val actionGrid: ImageVector = Icons.Rounded.GridOn
    override val actionPalette: ImageVector = Icons.Rounded.Collections
    override val actionAdd: ImageVector = Icons.Rounded.Add
    override val actionRemove: ImageVector = Icons.Rounded.Remove
    override val actionEdit: ImageVector = Icons.Rounded.Edit
    override val actionClean: ImageVector = Icons.Rounded.CleaningServices
    override val actionZoomIn: ImageVector = Icons.Rounded.ZoomIn

    override val navStudio: ImageVector = Icons.Rounded.Tune
    override val navEditor: ImageVector = Icons.Rounded.AutoFixHigh
    override val navPalettes: ImageVector = Icons.Rounded.Palette
    override val navProfile: ImageVector = Icons.Rounded.PhotoLibrary
    override val navSettings: ImageVector = Icons.Rounded.Settings

    override val statusCheck: ImageVector = Icons.Rounded.Check
    override val statusCheckCircle: ImageVector = Icons.Rounded.CheckCircle
    override val statusInfo: ImageVector = Icons.Rounded.Info
    override val statusChevronDown: ImageVector = Icons.Rounded.KeyboardArrowDown
    override val statusChevronRight: ImageVector = Icons.Rounded.ChevronRight
    override val statusArrowBack: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    override val statusArrowForward: ImageVector = Icons.AutoMirrored.Rounded.ArrowForward
    override val statusArrowDropDown: ImageVector = Icons.Rounded.ArrowDropDown
    override val statusArrowLeft: ImageVector = Icons.Rounded.ArrowLeft
    override val statusHistogram: ImageVector = Icons.Rounded.BarChart
    override val statusAspect: ImageVector = Icons.Rounded.AspectRatio
    override val statusBrightness: ImageVector = Icons.Rounded.BrightnessMedium
    override val statusMovieFilter: ImageVector = Icons.Rounded.MovieFilter
    override val statusShutterSpeed: ImageVector = Icons.Rounded.ShutterSpeed
    override val statusSpeed: ImageVector = Icons.Rounded.Speed
    override val statusFace: ImageVector = Icons.Rounded.Face
    override val statusLock: ImageVector = Icons.Rounded.Lock
    override val statusLanguage: ImageVector = Icons.Rounded.Language
}

/**
 * Secondary interchangeable icon set: Material Outlined icons (Technical / Minimalist aesthetic).
 * Proves 100% decoupling between UI screens and concrete icon assets.
 */
object OutlinedSzlIconSet : SzlIconSet {
    override val toolBasic: ImageVector = Icons.Outlined.Tune
    override val toolToneCurve: ImageVector = Icons.AutoMirrored.Outlined.ShowChart
    override val toolColorMixer: ImageVector = Icons.Outlined.Palette
    override val toolColorGrading: ImageVector = Icons.Outlined.ColorLens
    override val toolDetail: ImageVector = Icons.Outlined.Details
    override val toolEffects: ImageVector = Icons.Outlined.AutoAwesome
    override val toolCalibration: ImageVector = Icons.Outlined.Colorize
    override val toolCrop: ImageVector = Icons.Outlined.Crop
    override val toolGeometry: ImageVector = Icons.Outlined.Transform
    override val toolLens: ImageVector = Icons.Outlined.Camera

    override val actionExport: ImageVector = Icons.Outlined.FileDownload
    override val actionImport: ImageVector = Icons.Outlined.FileUpload
    override val actionSave: ImageVector = Icons.Outlined.Save
    override val actionUndo: ImageVector = Icons.AutoMirrored.Outlined.Undo
    override val actionRedo: ImageVector = Icons.AutoMirrored.Outlined.Redo
    override val actionCompare: ImageVector = Icons.Outlined.Compare
    override val actionAutoEdit: ImageVector = Icons.Outlined.AutoFixHigh
    override val actionReset: ImageVector = Icons.Outlined.RestartAlt
    override val actionRefresh: ImageVector = Icons.Outlined.Refresh
    override val actionMenu: ImageVector = Icons.Outlined.MoreVert
    override val actionHelp: ImageVector = Icons.AutoMirrored.Outlined.HelpOutline
    override val actionBack: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack
    override val actionSearch: ImageVector = Icons.Outlined.Search
    override val actionChevronRight: ImageVector = Icons.Outlined.ChevronRight
    override val actionDelete: ImageVector = Icons.Outlined.DeleteOutline
    override val actionShare: ImageVector = Icons.Outlined.Share
    override val actionVisibilityOn: ImageVector = Icons.Outlined.Visibility
    override val actionVisibilityOff: ImageVector = Icons.Outlined.VisibilityOff
    override val actionClose: ImageVector = Icons.Outlined.Close
    override val actionAddPhoto: ImageVector = Icons.Outlined.AddPhotoAlternate
    override val actionDuplicate: ImageVector = Icons.Outlined.ContentCopy
    override val actionCamera: ImageVector = Icons.Outlined.CameraAlt
    override val actionGrid: ImageVector = Icons.Outlined.GridOn
    override val actionPalette: ImageVector = Icons.Outlined.Collections
    override val actionAdd: ImageVector = Icons.Outlined.Add
    override val actionRemove: ImageVector = Icons.Outlined.Remove
    override val actionEdit: ImageVector = Icons.Outlined.Edit
    override val actionClean: ImageVector = Icons.Outlined.CleaningServices
    override val actionZoomIn: ImageVector = Icons.Outlined.ZoomIn

    override val navStudio: ImageVector = Icons.Outlined.Tune
    override val navEditor: ImageVector = Icons.Outlined.AutoFixHigh
    override val navPalettes: ImageVector = Icons.Outlined.Palette
    override val navProfile: ImageVector = Icons.Outlined.PhotoLibrary
    override val navSettings: ImageVector = Icons.Outlined.Settings

    override val statusCheck: ImageVector = Icons.Outlined.Check
    override val statusCheckCircle: ImageVector = Icons.Outlined.CheckCircle
    override val statusInfo: ImageVector = Icons.Outlined.Info
    override val statusChevronDown: ImageVector = Icons.Outlined.KeyboardArrowDown
    override val statusChevronRight: ImageVector = Icons.Outlined.ChevronRight
    override val statusArrowBack: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack
    override val statusArrowForward: ImageVector = Icons.AutoMirrored.Outlined.ArrowForward
    override val statusArrowDropDown: ImageVector = Icons.Outlined.ArrowDropDown
    override val statusArrowLeft: ImageVector = Icons.Outlined.ArrowLeft
    override val statusHistogram: ImageVector = Icons.Outlined.BarChart
    override val statusAspect: ImageVector = Icons.Outlined.AspectRatio
    override val statusBrightness: ImageVector = Icons.Outlined.BrightnessMedium
    override val statusMovieFilter: ImageVector = Icons.Outlined.MovieFilter
    override val statusShutterSpeed: ImageVector = Icons.Outlined.ShutterSpeed
    override val statusSpeed: ImageVector = Icons.Outlined.Speed
    override val statusFace: ImageVector = Icons.Outlined.Face
    override val statusLock: ImageVector = Icons.Outlined.Lock
    override val statusLanguage: ImageVector = Icons.Outlined.Language
}

val LocalSzlIcons = staticCompositionLocalOf<SzlIconSet> { RoundedSzlIconSet }
val LocalSzlIconSet = LocalSzlIcons

val SzlTheme.icons: SzlIconSet
    @Composable
    @ReadOnlyComposable
    get() = LocalSzlIcons.current

val MaterialTheme.icons: SzlIconSet
    @Composable
    @ReadOnlyComposable
    get() = LocalSzlIcons.current

