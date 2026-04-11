package compose.iot.util

import timber.log.Timber

/**
 * 统一错误处理封装
 * 用于替代散乱的 try-catch 模式，提供类型安全的结果处理
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : AppResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Error -> null
        }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T =
        when (this) {
            is Success -> data
            is Error -> defaultValue
        }
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (String, Throwable?) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(message, exception)
    return this
}

/**
 * 安全执行代码块，自动捕获异常并记录日志
 */
inline fun <T> safeCall(
    errorMessage: String = "操作失败",
    block: () -> T,
): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: Exception) {
        Timber.e(e, errorMessage)
        AppResult.Error(e.message ?: errorMessage, e)
    }
