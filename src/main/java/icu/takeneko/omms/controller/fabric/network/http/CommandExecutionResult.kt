package icu.takeneko.omms.controller.fabric.network.http
data class CommandExecutionResult(
    val controllerId:String,
    val command:String,
    val result:List<String>,
    val status: Boolean,
    val exceptionMessage: String?,
    val exceptionDetail: String
){
    override fun toString(): String {
        return "CommandExecutionResult(result=$result, status=$status, exceptionMessage='$exceptionMessage', exceptionDetail='$exceptionDetail')"
    }
}