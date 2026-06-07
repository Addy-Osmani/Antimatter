package dev.saifmukhtar.antimatter.network

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
        val conversationId: String?,
        val model: String,
        val stepCount: Int,
        val cloudflareUrl: String?
    ) : InboundMessage()

    data class Step(
        val step: TrajectoryStep,
        val index: Int
    ) : InboundMessage()

    data class Generating(val conversationId: String) : InboundMessage()
    data class ResponseComplete(val conversationId: String) : InboundMessage()

    data class ActiveFile(
        val path: String,
        val language: String
    ) : InboundMessage()

    data class FileContent(
        val path: String,
        val content: String,
        val language: String
    ) : InboundMessage()

    data class FileTree(val tree: List<FileNode>) : InboundMessage()
    data class CloudflareUrl(val url: String) : InboundMessage()
    data class Error(val message: String) : InboundMessage()
    data class HistoryList(val conversations: List<ConversationSummary>) : InboundMessage()
    object Unknown : InboundMessage()
}

// ─────────────────────────────────────────────────────────────────────────────
//  OUTBOUND MESSAGES (Android → PC)
// ─────────────────────────────────────────────────────────────────────────────

sealed class OutboundMessage(val type: String) {
    data class SendMessage(val text: String) : OutboundMessage("SEND_MESSAGE")
    class NewConversation : OutboundMessage("NEW_CONVERSATION")
    class CancelResponse : OutboundMessage("CANCEL_RESPONSE")
    class AcceptEdits : OutboundMessage("ACCEPT_EDITS")
    class RejectEdits : OutboundMessage("REJECT_EDITS")
    class ChangeModel : OutboundMessage("CHANGE_MODEL")
    class NextHunk : OutboundMessage("NEXT_HUNK")
    class PrevHunk : OutboundMessage("PREV_HUNK")
    class AcceptHunk : OutboundMessage("ACCEPT_HUNK")
    class RejectHunk : OutboundMessage("REJECT_HUNK")
    data class GetFiles(val path: String? = null) : OutboundMessage("GET_FILES")
    data class ReadFile(val path: String) : OutboundMessage("READ_FILE")
    data class SubscribeConversation(val conversationId: String) : OutboundMessage("SUBSCRIBE_CONVERSATION")
    class GetHistory : OutboundMessage("GET_HISTORY")
    class Ping : OutboundMessage("PING")
}

// ─────────────────────────────────────────────────────────────────────────────
//  FILE SYSTEM
// ─────────────────────────────────────────────────────────────────────────────

data class FileNode(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val children: List<FileNode>? = null
)

data class ConversationSummary(
    val id: String,
    val timestamp: Long,
    val title: String
)
