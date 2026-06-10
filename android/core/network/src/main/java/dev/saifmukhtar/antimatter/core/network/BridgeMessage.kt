package dev.saifmukhtar.antimatter.core.network

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
//  PROTOCOL — Messages over the WebSocket bridge
// ─────────────────────────────────────────────────────────────────────────────

/** Step types that come from Antigravity's trajectory */
enum class StepCase(val raw: String) {
    USER_INPUT("userInput"),
    PLANNER_RESPONSE("plannerResponse"),
    MARKDOWN_CHUNK("markdownChunk"),
    TEXT("text"),
    TOOL_CALL("toolCall"),
    RUN_COMMAND("runCommand"),
    ERROR_MESSAGE("errorMessage"),
    EPHEMERAL_MESSAGE("ephemeralMessage"),
    CHECKPOINT("checkpoint"),
    TASK_BOUNDARY("taskBoundary"),
    INVOKE_SUBAGENT("invokeSubagent"),
    SEND_MESSAGE("sendMessage"),
    APPROVAL_INTERACTION("approvalInteraction"),
    ELICITATION("elicitation"),
    ASK_QUESTION("askQuestion"),
    UNKNOWN("unknown");

    companion object {
        fun from(raw: String) = entries.firstOrNull { 
            it.name.equals(raw, ignoreCase = true) || it.raw.equals(raw, ignoreCase = true) 
        } ?: UNKNOWN
    }
}

/** A single step in the AI conversation trajectory */
data class TrajectoryStep(
    @SerializedName("case") val case: String = "unknown",
    @SerializedName("value") val value: String? = null,
    @SerializedName("tool") val tool: String? = null,
    @SerializedName("command") val command: String? = null,
) {
    val stepCase: StepCase get() = StepCase.from(case)

    /** Human-readable display text for this step */
    val displayText: String
        get() = when (stepCase) {
            StepCase.USER_INPUT -> value ?: ""
            StepCase.PLANNER_RESPONSE -> value ?: ""
            StepCase.MARKDOWN_CHUNK, StepCase.TEXT -> value ?: ""
            StepCase.TOOL_CALL -> "Using tool: ${tool ?: "unknown"}"
            StepCase.RUN_COMMAND -> "$ ${command ?: value ?: ""}"
            StepCase.ERROR_MESSAGE -> "⚠️ ${value ?: "Error"}"
            StepCase.EPHEMERAL_MESSAGE -> value ?: ""
            StepCase.APPROVAL_INTERACTION -> "⏸ Waiting for approval"
            StepCase.ELICITATION, StepCase.ASK_QUESTION -> "❓ ${value ?: "Question"}"
            else -> value ?: ""
        }
}

// ─────────────────────────────────────────────────────────────────────────────
//  INBOUND MESSAGES (PC → Android)
// ─────────────────────────────────────────────────────────────────────────────

sealed class InboundMessage {
    data class Pong(val dummy: Unit = Unit) : InboundMessage()

    data class SessionState(
        val conversationId: String? = null,
        val model: String = "",
        val stepCount: Int = 0,
        val cloudflareUrl: String? = null
    ) : InboundMessage()

    data class Step(
        val step: TrajectoryStep = TrajectoryStep(),
        val index: Int = 0
    ) : InboundMessage()

    data class StepBatch(val steps: List<Step> = emptyList()) : InboundMessage()

    data class Generating(val conversationId: String = "") : InboundMessage()
    data class ResponseComplete(val conversationId: String = "") : InboundMessage()

    data class ActiveFile(
        val path: String = "",
        val language: String = ""
    ) : InboundMessage()

    data class FileContent(
        val path: String = "",
        val content: String = "",
        val language: String = ""
    ) : InboundMessage()

    data class FileTree(val tree: List<FileNode> = emptyList()) : InboundMessage()
    data class CloudflareUrl(val url: String = "") : InboundMessage()
    data class Error(val message: String = "") : InboundMessage()
    data class SystemAlert(val title: String = "", val body: String = "") : InboundMessage()
    data class TerminalOutput(@SerializedName("content") val content: String = "") : InboundMessage()
    data class HistoryList(val conversations: List<ConversationSummary> = emptyList()) : InboundMessage()
    data class AuthResponse(val signature: String = "") : InboundMessage()
    data class ArtifactsList(val artifacts: List<FileNode> = emptyList()) : InboundMessage()
    data class CommandOutput(val text: String = "", val isError: Boolean = false) : InboundMessage()
    object Unknown : InboundMessage()
}

// ─────────────────────────────────────────────────────────────────────────────
//  OUTBOUND MESSAGES (Android → PC)
// ─────────────────────────────────────────────────────────────────────────────

sealed class OutboundMessage {
    data class SendMessage(val text: String, val type: String = "SEND_MESSAGE") : OutboundMessage()
    data class NewConversation(val type: String = "NEW_CONVERSATION") : OutboundMessage()
    data class CancelResponse(val type: String = "CANCEL_RESPONSE") : OutboundMessage()
    data class AcceptEdits(val type: String = "ACCEPT_EDITS") : OutboundMessage()
    data class RejectEdits(val type: String = "REJECT_EDITS") : OutboundMessage()
    data class ChangeModel(val type: String = "CHANGE_MODEL") : OutboundMessage()
    data class NextHunk(val type: String = "NEXT_HUNK") : OutboundMessage()
    data class PrevHunk(val type: String = "PREV_HUNK") : OutboundMessage()
    data class AcceptHunk(val type: String = "ACCEPT_HUNK") : OutboundMessage()
    data class RejectHunk(val type: String = "REJECT_HUNK") : OutboundMessage()
    data class GetFiles(val path: String? = null, val type: String = "GET_FILES") : OutboundMessage()
    data class ReadFile(val path: String, val type: String = "READ_FILE") : OutboundMessage()
    data class SubscribeConversation(
        val conversationId: String,
        val lastKnownStepCount: Int? = null,
        val type: String = "SUBSCRIBE_CONVERSATION"
    ) : OutboundMessage()
    data class GetHistory(val type: String = "GET_HISTORY") : OutboundMessage()
    data class Ping(val type: String = "PING") : OutboundMessage()
    data class AuthChallenge(val challenge: String, val type: String = "AUTH_CHALLENGE") : OutboundMessage()
    data class GetArtifacts(val conversationId: String, val type: String = "GET_ARTIFACTS") : OutboundMessage()
    
    data class WriteFile(val path: String, val content: String, val type: String = "WRITE_FILE") : OutboundMessage()
    data class ExecuteCommand(val command: String, val type: String = "EXECUTE_COMMAND") : OutboundMessage()
}

// ─────────────────────────────────────────────────────────────────────────────
//  FILE SYSTEM
// ─────────────────────────────────────────────────────────────────────────────

data class FileNode(
    val name: String = "",
    val path: String = "",
    val isDir: Boolean = false,
    val children: List<FileNode>? = null
)

data class ConversationSummary(
    val id: String = "",
    val timestamp: Long = 0L,
    val title: String = ""
)
