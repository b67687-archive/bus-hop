package com.bushop.sg.domain.model

enum class ColorSchemeOption(
    val rawValue: String,
    val displayName: String,
) {
    BLUE("blue", "Blue Classic"),
    CONTRAST_BLUE("contrast_blue", "Contrast Blue"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): ColorSchemeOption = entries.firstOrNull { it.rawValue == rawValue } ?: BLUE
    }
}
