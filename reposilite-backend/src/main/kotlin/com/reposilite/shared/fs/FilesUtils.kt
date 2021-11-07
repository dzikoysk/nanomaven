/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reposilite.shared.fs

import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.regex.Pattern
import kotlin.math.abs

internal object FilesUtils {

    private val DISPLAY_SIZE_PATTERN = Pattern.compile("([0-9]+)(([KkMmGg])[Bb])")

    private const val KB_FACTOR: Long = 1024
    private const val MB_FACTOR = 1024 * KB_FACTOR
    private const val GB_FACTOR = 1024 * MB_FACTOR

    fun displaySizeToBytesCount(displaySize: String): Long {
        val match = DISPLAY_SIZE_PATTERN.matcher(displaySize)

        if (!match.matches() || match.groupCount() != 3) {
            return displaySize.toLong()
        }

        val value = match.group(1).toLong()

        return when (match.group(2).uppercase()) {
            "GB" -> value * GB_FACTOR
            "MB" -> value * MB_FACTOR
            "KB" -> value * KB_FACTOR
            else -> throw NumberFormatException("Wrong format")
        }
    }

    // Source
    // ~ https://stackoverflow.com/a/3758880/3426515
    fun humanReadableByteCount(bytes: Long): String {
        val absB =
            if (bytes == Long.MIN_VALUE) Long.MAX_VALUE
            else abs(bytes)

        if (absB < 1024) {
            return "$bytes B"
        }

        var value = absB
        val characterIterator: CharacterIterator = StringCharacterIterator("KMGTPE")
        var i = 40

        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            characterIterator.next()
            i -= 10
        }

        value *= java.lang.Long.signum(bytes).toLong()
        return String.format("%.1f %ciB", value / 1024.0, characterIterator.current())
    }

}

fun String.getExtension(): String =
    lastIndexOf(".")
        .takeIf { it != -1 }
        ?.let { substring(it + 1) }
        ?: ""