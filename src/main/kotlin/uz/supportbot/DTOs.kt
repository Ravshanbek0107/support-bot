package uz.supportbot

data class BaseMessage(val code: Int? = null, val message: String? = null) {
    companion object {
        var OK = BaseMessage(0, "OK")
    }
}

data class MessageValidationResult(
    val isValid: Boolean,
    val errorKey: String? = null
)

data class OperatorCreateRequest(
    val phoneNumber: String
)

data class OperatorResponse(
    val username: String?,
    val fullname: String,
    val phoneNumber: String?,
    val role: UserRole,
){
    companion object {
        fun toResponse(operator: User) = OperatorResponse(
            username = operator.username,
            fullname = operator.fullName,
            phoneNumber = operator.phoneNumber,
            role = operator.role,
        )
    }
}

data class OperatorStatResponse(
    val operatorId: Long,
    val username: String?,
    val fullName: String,
    val phoneNumber: String,
    val avgRating: Double?,
    val ratingCount: Long?
)

data class AllOperatorStatsResponse(
    val operators: List<OperatorStatResponse>
)