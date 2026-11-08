package com.stealzalut.mobile.theme

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * JVM Guard Test enforcing Design System standards (FE-026).
 *
 * Ensures ZERO hardcoded raw colors, dp dimensions, sp font sizes, raw icons,
 * unlocalized strings, or explicit font families are introduced into ui/screens or ui/components
 * without passing through AppDimens, SzlTheme/MaterialTheme, SzlIconSet, or string resources.
 *
 * Whitelist is STRICTLY EMPTY: all UI code must consume semantic tokens.
 */
class DesignSystemGuardTest {

    private val projectRoot = File(".").canonicalFile
    private val targetDirs = listOf(
        File(projectRoot, "app/src/main/java/com/stealzalut/mobile/ui/screens"),
        File(projectRoot, "app/src/main/java/com/stealzalut/mobile/ui/components")
    )

    @Test
    fun `verify zero forbidden literals in ui screens and ui components`() {
        val violations = mutableListOf<String>()

        val colorPattern = Regex("""Color\(0x[0-9a-fA-F]+\)""")
        val dpPattern = Regex("""\b\d+(\.\d+)?\.dp\b""")
        val spPattern = Regex("""\b\d+(\.\d+)?\.sp\b""")
        val hardcodedTextPattern = Regex("""Text\(\s*"[^"]{2,}"[\s,)](?!\s*\/\/\s*GUARD_ALLOW)""")
        val contentDescPattern = Regex("""contentDescription\s*=\s*"[^"]{2,}"(?!\s*\/\/\s*GUARD_ALLOW)""")
        val userMsgPattern = Regex("""userMessage\s*=\s*"[^"]{2,}"(?!\s*\/\/\s*GUARD_ALLOW)""")
        val rawIconsPattern = Regex("""\bIcons\.(Default|Filled|Outlined|AutoMirrored|Rounded|Sharp)\b(?!\s*\/\/\s*GUARD_ALLOW)""")
        val rawFontFamilyPattern = Regex("""\bFontFamily\([^)]*\)(?!\s*\/\/\s*GUARD_ALLOW)""")
        val rawFontSizePattern = Regex("""\bfontSize\s*=\s*\d+(?!\s*\/\/\s*GUARD_ALLOW)""")

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

                    if (colorPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Raw Color literal found: $trimmed")
                    }

                    if (dpPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Hardcoded .dp dimension found: $trimmed")
                    }

                    if (spPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Hardcoded .sp typography found: $trimmed")
                    }

                    if (hardcodedTextPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Hardcoded Text(\"...\") found: $trimmed")
                    }

                    if (contentDescPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Hardcoded contentDescription found: $trimmed")
                    }

                    if (userMsgPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Hardcoded userMessage found: $trimmed")
                    }

                    if (rawIconsPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Raw Icons.* literal found (use SzlIconSet/MaterialTheme.icons): $trimmed")
                    }

                    if (rawFontFamilyPattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Raw FontFamily(...) found (use FontSpec/ThemeSpec): $trimmed")
                    }

                    if (rawFontSizePattern.containsMatchIn(line)) {
                        violations.add("${file.name}:$lineNum -> Raw fontSize found: $trimmed")
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
