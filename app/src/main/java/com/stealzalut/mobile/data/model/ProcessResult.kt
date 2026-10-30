package com.stealzalut.mobile.data.model

import androidx.compose.runtime.Immutable

/**
 * Domain-specific error hierarchy bridging Python Chaquopy and Kotlin Android.
 */
@Immutable
sealed interface LutDomainError {
    @Immutable data object SourceImageNotFound : LutDomainError
    @Immutable data object ReferenceImageNotFound : LutDomainError
    @Immutable data object MatrixSingularityError : LutDomainError
    @Immutable data object OutOfMemoryError : LutDomainError
    @Immutable data class ExportIoError(val details: String) : LutDomainError
    @Immutable data class GenericError(val details: String) : LutDomainError

    val userMessage: String
        get() = when (this) {
            is SourceImageNotFound -> "Image source introuvable ou inaccessible."
            is ReferenceImageNotFound -> "Image de référence introuvable ou inaccessible."
            is MatrixSingularityError -> "Singularité matricielle détectée (image source ou référence trop uniforme)."
            is OutOfMemoryError -> "Mémoire insuffisante pour effectuer le traitement haute résolution."
            is ExportIoError -> "Erreur d'écriture ou d'accès disque : $details"
            is GenericError -> details.ifBlank { "Une erreur inattendue est survenue." }
        }

    companion object {
        fun fromPythonResponse(errorCode: String?, rawMessage: String?): LutDomainError {
            val msg = rawMessage ?: ""
            return when (errorCode) {
                "SOURCE_NOT_FOUND" -> SourceImageNotFound
                "REFERENCE_NOT_FOUND" -> ReferenceImageNotFound
                "MATRIX_SINGULARITY" -> MatrixSingularityError
                "OUT_OF_MEMORY" -> OutOfMemoryError
                "EXPORT_IO_ERROR" -> ExportIoError(msg)
                else -> {
                    val lower = msg.lowercase()
                    when {
                        "source" in lower && "not found" in lower -> SourceImageNotFound
                        "reference" in lower && "not found" in lower -> ReferenceImageNotFound
                        "singul" in lower || "linalg" in lower -> MatrixSingularityError
                        "memory" in lower || "oom" in lower -> OutOfMemoryError
                        "io" in lower || "permission" in lower || "decode" in lower -> ExportIoError(msg)
                        else -> GenericError(msg)
                    }
                }
            }
        }
    }
}

/**
 * Result model returned when generating a color-transfer preview.
 */
@Immutable
sealed interface PreviewResult {
    @Immutable
    data class Success(
        val previewPath: String,
        val executionTimeMs: Double
    ) : PreviewResult

    @Immutable
    data class Error(
        val error: LutDomainError,
        val customMessage: String? = null
    ) : PreviewResult {
        val message: String get() = customMessage ?: error.userMessage
        constructor(message: String) : this(LutDomainError.GenericError(message), message)
    }
}

/**
 * Result model returned when generating an Adobe .cube 3D LUT file.
 */
@Immutable
sealed interface CubeExportResult {
    @Immutable
    data class Success(
        val cubePath: String,
        val lutSize: Int,
        val executionTimeMs: Double
    ) : CubeExportResult

    @Immutable
    data class Error(
        val error: LutDomainError,
        val customMessage: String? = null
    ) : CubeExportResult {
        val message: String get() = customMessage ?: error.userMessage
        constructor(message: String) : this(LutDomainError.GenericError(message), message)
    }
}

/**
 * Result model returned when directly exporting a high-resolution graded image.
 */
@Immutable
sealed interface ImageExportResult {
    @Immutable
    data class Success(
        val imagePath: String,
        val width: Int,
        val height: Int,
        val executionTimeMs: Double
    ) : ImageExportResult

    @Immutable
    data class Error(
        val error: LutDomainError,
        val customMessage: String? = null
    ) : ImageExportResult {
        val message: String get() = customMessage ?: error.userMessage
        constructor(message: String) : this(LutDomainError.GenericError(message), message)
    }
}
