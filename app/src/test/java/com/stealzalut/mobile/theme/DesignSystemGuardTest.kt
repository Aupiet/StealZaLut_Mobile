package com.stealzalut.mobile.theme

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * JVM Guard Test enforcing Design System standards.
 *
 * Ensures no hardcoded raw colors, dp dimensions, sp font sizes, or unlocalized strings
 * are introduced into ui/screens or ui/components without passing through AppDimens,
 * SzlTheme/MaterialTheme, or string resources.
 */
class DesignSystemGuardTest {

    private val projectRoot = File(".").canonicalFile
    private val targetDirs = listOf(
        File(projectRoot, "app/src/main/java/com/stealzalut/mobile/ui/screens"),
        File(projectRoot, "app/src/main/java/com/stealzalut/mobile/ui/components")
    )

    // Explicit documented exceptions undergoing step-by-step Phase 2 refactoring
    private val allowedExceptions = setOf(
        // Calibration mire physical patches to be redirected to ColorCardReference in Phase 2
        "ProfileScreen.kt:Color(0x",
        "PalettesScreen.kt:Color(0x",
        // Legacy dimensions being migrated to AppDimens in Phase 2
        "ProfileScreen.kt:.dp",
        "ProfileScreen.kt:.sp",
        "PalettesScreen.kt:.dp",
        "StandaloneEditorScreen.kt:.dp",
        "ImageDetailDialog.kt:.dp",
        // Hardcoded strings to be externalized to strings.xml in Phase 2
        "StandaloneEditorScreen.kt:Text(\"",
        "StandaloneEditorScreen.kt:contentDescription = \"",
        "ProfileScreen.kt:contentDescription = \"",
        "PalettesScreen.kt:contentDescription = \"",
        "ImageDetailDialog.kt:Text(\"",
        "ImageDetailDialog.kt:contentDescription = \"",
        "MethodSelector.kt:contentDescription = \"",
        "LutSizeSelector.kt:contentDescription = \""
    )

    private fun isWhitelisted(fileName: String, violationType: String): Boolean {
        return allowedExceptions.contains("$fileName:$violationType")
    }

    @Test
    fun `verify zero forbidden literals in ui screens and ui components`() {
        val violations = mutableListOf<String>()

        val colorPattern = Regex("""Color\(0x[0-9a-fA-F]+\)""")
        val dpPattern = Regex("""\b\d+(\.\d+)?\.dp\b""")
        val spPattern = Regex("""\b\d+(\.\d+)?\.sp\b""")
        val hardcodedTextPattern = Regex("""Text\(\s*"[^"]{2,}"[\s,)](?!\s*\/\/\s*GUARD_ALLOW)""")
        val contentDescPattern = Regex("""contentDescription\s*=\s*"[^"]{2,}"(?!\s*\/\/\s*GUARD_ALLOW)""")
        val userMsgPattern = Regex("""userMessage\s*=\s*"[^"]{2,}"(?!\s*\/\/\s*GUARD_ALLOW)""")

        targetDirs.forEach { dir ->
            if (!dir.exists()) return@forEach
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    val lineNum = index + 1
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                        return@forEachIndexed
                    }

                    if (colorPattern.containsMatchIn(line) && !isWhitelisted(file.name, "Color(0x")) {
                        violations.add("${file.name}:$lineNum -> Raw Color literal found: $trimmed")
                    }

                    if (dpPattern.containsMatchIn(line) && !isWhitelisted(file.name, ".dp")) {
                        violations.add("${file.name}:$lineNum -> Hardcoded .dp dimension found: $trimmed")
                    }

                    if (spPattern.containsMatchIn(line) && !isWhitelisted(file.name, ".sp")) {
                        violations.add("${file.name}:$lineNum -> Hardcoded .sp typography found: $trimmed")
                    }

                    if (hardcodedTextPattern.containsMatchIn(line) && !isWhitelisted(file.name, "Text(\"")) {
                        violations.add("${file.name}:$lineNum -> Hardcoded Text(\"...\") found: $trimmed")
                    }

                    if (contentDescPattern.containsMatchIn(line) && !isWhitelisted(file.name, "contentDescription = \"")) {
                        violations.add("${file.name}:$lineNum -> Hardcoded contentDescription found: $trimmed")
                    }

                    if (userMsgPattern.containsMatchIn(line) && !isWhitelisted(file.name, "userMessage = \"")) {
                        violations.add("${file.name}:$lineNum -> Hardcoded userMessage found: $trimmed")
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Design System Guard violations detected (${violations.size}):\n" + violations.joinToString("\n")
        )
    }
}
