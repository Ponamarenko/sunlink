package com.sunlink.svitgrid.data.model

/**
 * Wrapper for successful or failed operations.
 * Similar to Kotlin's Result but with specific types for our use case.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()

    /**
     * Returns the value if this is a Success, otherwise throws the exception.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }

    /**
     * Maps the value if this is a Success.
     */
    inline fun <U> map(transform: (T) -> U): Result<U> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
    }

    /**
     * FlatMaps the value if this is a Success.
     */
    inline fun <U> flatMap(transform: (T) -> Result<U>): Result<U> = when (this) {
        is Success -> transform(data)
        is Error -> Error(exception)
    }
}

/**
 * Creates a Success result.
 */
fun <T> success(data: T): Result<T> = Result.Success(data)

/**
 * Creates an Error result.
 */
fun <T> error(exception: Exception): Result<T> = Result.Error(exception)