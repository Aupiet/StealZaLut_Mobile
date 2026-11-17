package com.stealzalut.engine.preset

object SimpleJson {

    fun parse(json: String): Any? {
        val src = json.trim()
        var pos = 0

        fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        fun parseString(): String {
            pos++ // skip leading '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    if (pos >= src.length) break
                    val esc = src[pos++]
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val uHex = src.substring(pos, pos + 4)
                            sb.append(uHex.toInt(16).toChar())
                            pos += 4
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        fun parseNumber(): Number {
            val start = pos
            if (pos < src.length && src[pos] == '-') pos++
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
                pos++
            }
            val numStr = src.substring(start, pos)
            return if (numStr.contains('.') || numStr.contains('e') || numStr.contains('E')) {
                numStr.toDouble()
            } else {
                numStr.toLong()
            }
        }

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= src.length) return null
            return when (src[pos]) {
                '{' -> {
                    pos++
                    val map = LinkedHashMap<String, Any?>()
                    while (true) {
                        skipWhitespace()
                        if (pos >= src.length || src[pos] == '}') {
                            if (pos < src.length) pos++
                            break
                        }
                        val key = parseString()
                        skipWhitespace()
                        if (pos < src.length && src[pos] == ':') pos++
                        val value = parseValue()
                        map[key] = value
                        skipWhitespace()
                        if (pos < src.length && src[pos] == ',') {
                            pos++
                        } else if (pos < src.length && src[pos] == '}') {
                            pos++
                            break
                        }
                    }
                    map
                }
                '[' -> {
                    pos++
                    val list = ArrayList<Any?>()
                    while (true) {
                        skipWhitespace()
                        if (pos >= src.length || src[pos] == ']') {
                            if (pos < src.length) pos++
                            break
                        }
                        val value = parseValue()
                        list.add(value)
                        skipWhitespace()
                        if (pos < src.length && src[pos] == ',') {
                            pos++
                        } else if (pos < src.length && src[pos] == ']') {
                            pos++
                            break
                        }
                    }
                    list
                }
                '"' -> parseString()
                't', 'f' -> {
                    if (src.startsWith("true", pos)) {
                        pos += 4
                        true
                    } else if (src.startsWith("false", pos)) {
                        pos += 5
                        false
                    } else null
                }
                'n' -> {
                    if (src.startsWith("null", pos)) pos += 4
                    null
                }
                else -> parseNumber()
            }
        }

        return parseValue()
    }

    fun serialize(obj: Any?, indent: Int = 2, currentIndent: Int = 0): String {
        val ind = " ".repeat(currentIndent)
        val nextInd = " ".repeat(currentIndent + indent)

        return when (obj) {
            null -> "null"
            is String -> "\"${escape(obj)}\""
            is Number -> if (obj is Double && obj == obj.toLong().toDouble()) "${obj.toLong()}.0" else obj.toString()
            is Boolean -> obj.toString()
            is Map<*, *> -> {
                if (obj.isEmpty()) return "{}"
                val entries = obj.entries.joinToString(",\n") { (k, v) ->
                    "$nextInd\"${escape(k.toString())}\": ${serialize(v, indent, currentIndent + indent)}"
                }
                "{\n$entries\n$ind}"
            }
            is List<*> -> {
                if (obj.isEmpty()) return "[]"
                val items = obj.joinToString(",\n") { v ->
                    "$nextInd${serialize(v, indent, currentIndent + indent)}"
                }
                "[\n$items\n$ind]"
            }
            is Array<*> -> serialize(obj.toList(), indent, currentIndent)
            else -> "\"${escape(obj.toString())}\""
        }
    }

    private fun escape(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000c' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
