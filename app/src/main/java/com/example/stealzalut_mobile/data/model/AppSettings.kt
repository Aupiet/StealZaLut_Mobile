package com.example.stealzalut_mobile.data.model

/**
 * Global application preferences and processing parameters.
 *
 * @property defaultLutSize Default grid size for 3D LUT generation
 * @property defaultMethod Default color transfer method
 * @property analysisProxyDim Proxy dimension for statistical analysis (256, 512, 1024)
 * @property maxExportDim Max dimension for direct image export (0 for full resolution, 2048, 3840)
 * @property exportFormat Direct image export format ("JPEG" or "PNG")
 * @property exportQuality JPEG compression quality (80-100)
 */
data class AppSettings(
    val defaultLutSize: LutSize = LutSize.STANDARD,
    val defaultMethod: ColorTransferMethod = ColorTransferMethod.HM_MKL_HM,
    val analysisProxyDim: Int = 512,
    val maxExportDim: Int = 2048,
    val exportFormat: String = "JPEG",
    val exportQuality: Int = 95
)
