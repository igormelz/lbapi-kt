package ru.openfs.lbapi.model

data class PasswordTemplate(
    val minLength: Long,
    val hasSpecialChars: Boolean,
    val hasUppercase: Boolean,
    val hasDigits: Boolean,
    val hasLowercase: Boolean,
    val checkPassword: Boolean,
    val excludeChars: String?
)
