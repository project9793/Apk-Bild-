package com.mayuri.ai.core.actions

/** Result of a device level action requested by the assistant. */
sealed class ActionResponse {
    data class Success(val message: String) : ActionResponse()
    data class NeedConfirmation(val message: String) : ActionResponse()
    data class Failure(val message: String) : ActionResponse()
}
