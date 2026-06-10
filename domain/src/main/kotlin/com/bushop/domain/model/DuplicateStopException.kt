package com.bushop.domain.model

/** Thrown when attempting to add a bus stop that already exists. */
class DuplicateStopException(
    message: String,
) : Exception(message)
