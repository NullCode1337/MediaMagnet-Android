package dev.nullcode.mediamagnet.data

import java.io.IOException

class ApiException(
    val status: Int,
    val code: String,
    message: String,
    val files: List<String> = emptyList(),
) : IOException(message)
