package dev.saifmukhtar.antimatter.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.saifmukhtar.antimatter.core.network.TrajectoryStep

@Entity(
    tableName = "steps",
    primaryKeys = ["conversationId", "stepIndex"],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class StepEntity(
    val conversationId: String,
    val stepIndex: Int,
    val type: String,
    val value: String?,
    val tool: String?,
    val command: String?
) {
    fun toTrajectoryStep() = TrajectoryStep(
        case = type,
        value = value,
        tool = tool,
        command = command
    )
}

fun TrajectoryStep.toEntity(conversationId: String, index: Int) = StepEntity(
    conversationId = conversationId,
    stepIndex = index,
    type = case,
    value = value,
    tool = tool,
    command = command
)
