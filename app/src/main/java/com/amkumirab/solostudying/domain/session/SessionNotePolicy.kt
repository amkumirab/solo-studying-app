package com.amkumirab.solostudying.domain.session

object SessionNotePolicy {
    const val MaxLength = 500

    fun normalize(value: String): String? = value
        .trim()
        .take(MaxLength)
        .ifBlank { null }
}
