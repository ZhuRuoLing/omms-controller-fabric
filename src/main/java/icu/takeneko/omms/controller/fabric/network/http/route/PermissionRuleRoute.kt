package icu.takeneko.omms.controller.fabric.network.http.route

import icu.takeneko.omms.controller.fabric.network.http.PermissionModificationData
import icu.takeneko.omms.controller.fabric.network.http.PermissionModificationResult
import icu.takeneko.omms.controller.fabric.permission.PermissionRuleManager
import icu.takeneko.omms.controller.fabric.util.Util.gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.permissionRule() {
    route("/permissionRule") {
        get("/switch/{operation?}") {
            val operation = when (call.parameters["operation"]) {
                "on" -> true
                "off" -> false
                null -> return@get call.respondText(
                    "Missing operation",
                    status = HttpStatusCode.BadRequest
                )

                else -> return@get call.respondText(
                    "Wrong operation value: ${call.parameters["operation"]}",
                    status = HttpStatusCode.BadRequest
                )
            }
            val clazz = call.request.queryParameters["className"] ?: return@get call.respondText(
                "Missing className",
                status = HttpStatusCode.BadRequest
            )
            val status = PermissionRuleManager.INSTANCE.permissionRuleMap[clazz]?.status
                ?: run { PermissionRuleManager.INSTANCE.createNewRule(clazz);false }
            PermissionRuleManager.INSTANCE.permissionRuleMap[clazz]!!.status = operation
            return@get call.respondText {
                if (status) "ENABLED" else "DISABLED"
            }
        }
        get("list") {
            val clazz = call.request.queryParameters["className"] ?: return@get call.respondText(
                status = HttpStatusCode.OK
            ) {
                gson.toJson(PermissionRuleManager.INSTANCE.permissionRuleMap)
            }
            val rule = PermissionRuleManager.INSTANCE.permissionRuleMap[clazz] ?: return@get
            call.respondText(status = HttpStatusCode.BadRequest) {
                gson.toJson(PermissionModificationResult(false, "Class not exist.", null))
            }
            return@get call.respondText {
                gson.toJson(PermissionModificationResult(true, "", rule))
            }
        }
        post("status") {
            val clazz = call.receiveText()
            val status =
                PermissionRuleManager.INSTANCE.permissionRuleMap[clazz]?.status ?: return@post call.respondText(
                    status = HttpStatusCode.BadRequest
                ) { gson.toJson(PermissionModificationResult(false, "Class not exist.", false)) }
            return@post call.respondText {
                gson.toJson(PermissionModificationResult(true, "", status))
            }
        }
        post("modify") {
            val content = call.receiveText()
            val dt = gson.fromJson(content, PermissionModificationData::class.java)
            try {
                when (dt.type) {
                    PermissionModificationData.Type.ENABLE -> {
                        PermissionRuleManager.INSTANCE.enableCheckFor(dt.className)
                    }

                    PermissionModificationData.Type.REMOVE -> {
                        PermissionRuleManager.INSTANCE.disableCheckFor(dt.className)
                    }

                    PermissionModificationData.Type.ADD_RULE -> {
                        PermissionRuleManager.INSTANCE.addRule(dt.className, dt.rule)
                    }

                    PermissionModificationData.Type.REMOVE_RULE -> {
                        PermissionRuleManager.INSTANCE.removeRule(dt.className, dt.removeAt)
                    }

                    else -> {
                        return@post call.respondText(
                            status = HttpStatusCode.BadRequest
                        ) {
                            gson.toJson(PermissionModificationResult(false, "Type not specified.", false))
                        }
                    }
                }
                return@post call.respondText(
                    status = HttpStatusCode.OK
                ) {
                    gson.toJson(PermissionModificationResult(true, "", null))
                }
            } catch (e: Exception) {
                return@post call.respondText(
                    status = HttpStatusCode.InternalServerError
                ) {
                    gson.toJson(
                        PermissionModificationResult(
                            false,
                            "Server Internal Error",
                            e.stackTraceToString()
                        )
                    )
                }
            }
        }
    }
}