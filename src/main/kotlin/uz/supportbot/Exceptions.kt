package uz.supportbot


import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.Locale

@ControllerAdvice
class ExceptionHandler(
    private val errorMessageSource: ResourceBundleMessageSource,
) {
    @ExceptionHandler(Throwable::class)
    fun handleOtherExceptions(exception: Throwable): ResponseEntity<Any> {
        when (exception) {
            is ShopAppException-> {

                return ResponseEntity
                    .badRequest()
                    .body(exception.getErrorMessage(errorMessageSource))
            }

            else -> {
                exception.printStackTrace()
                return ResponseEntity
                    .badRequest().body(
                        BaseMessage(100,
                            "Iltimos support bilan bog'laning")
                    )
            }
        }
    }

}



sealed class ShopAppException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorCode
    protected open fun getErrorMessageArguments(): Array<Any?>? = null
    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource): BaseMessage {
        return BaseMessage(
            errorType().code,
            errorMessageSource.getMessage(
                errorType().toString(),
                getErrorMessageArguments(),
                Locale(LocaleContextHolder.getLocale().language)
            )
        )
    }
}


class OperatorNotFoundException() : ShopAppException() {
    override fun errorType() = ErrorCode.OPERATOR_NOT_FOUND
}

class UserNotFoundException() : ShopAppException() {
    override fun errorType() = ErrorCode.USER_NOT_FOUND
}

class UserStateNotSuitableException() : ShopAppException() {
    override fun errorType() = ErrorCode.USER_STATE_NOT_SUITABLE
}

class OperatorStateNotSuitableException() : ShopAppException() {
    override fun errorType() = ErrorCode.OPERATOR_STATE_NOT_SUITABLE
}


