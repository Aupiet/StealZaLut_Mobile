package com.example.stealzalut_mobile.data.model

/**
 * Supported color-transfer algorithms in StealZaLUT.
 *
 * @property key Python bridge identifier
 * @property displayName User-friendly label for UI
 * @property description Explanatory text for the user
 */
enum class ColorTransferMethod(
    val key: String,
    val displayName: String,
    val description: String
) {
    HM_MKL_HM(
        key = "hm-mkl-hm",
        displayName = "HM + MKL (Défaut)",
        description = "Histogram Matching + Monge-Kantorovitch Linear : équilibre optimal et haute fidélité"
    ),
    MKL(
        key = "mkl",
        displayName = "MKL Pur",
        description = "Monge-Kantorovitch Linear : alignement optimal de la covariance des couleurs"
    ),
    HM(
        key = "hm",
        displayName = "Histogram Matching",
        description = "Correspondance d'histogramme canal par canal : rapide et direct"
    ),
    REINHARD(
        key = "reinhard",
        displayName = "Reinhard (LAB)",
        description = "Transfert statistique moyenne / variance dans l'espace perceptuel CIE LAB"
    );

    companion object {
        fun fromKey(key: String): ColorTransferMethod =
            entries.firstOrNull { it.key == key } ?: HM_MKL_HM
    }
}
