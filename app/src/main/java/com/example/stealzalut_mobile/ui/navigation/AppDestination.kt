package com.example.stealzalut_mobile.ui.navigation

/**
 * Top-level navigation destinations in StealZaLUT Pro:
 * 1. EXTRACT_THEME : Extraire la colorimétrie et le nuancier d'une image
 * 2. APPLY_THEME   : Appliquer un thème connu sur une photo (Visualisation Double Vue)
 * 3. PROFILE       : Dossier des thèmes sauvegardés (avec aperçu et export de mire) et des images étalonnées
 */
enum class AppDestination(
    val route: String,
    val label: String,
    val icon: String
) {
    EXTRACT_THEME(
        route = "extract_theme",
        label = "Extraire",
        icon = "🎨"
    ),
    APPLY_THEME(
        route = "apply_theme",
        label = "Appliquer",
        icon = "✨"
    ),
    PROFILE(
        route = "profile",
        label = "Profil & Dossiers",
        icon = "📁"
    )
}
