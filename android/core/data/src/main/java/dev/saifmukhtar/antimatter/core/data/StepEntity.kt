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
    val compressedValue: ByteArray?,
    val tool: String?,
    val command: String?
) {
    fun toTrajectoryStep(): TrajectoryStep {
        val decompressedValue = compressedValue?.let { GzipUtils.decompress(it) }
        return TrajectoryStep(
            case = type,
            value = decompressedValue,
            tool = tool,
            command = command
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StepEntity

        if (conversationId != other.conversationId) return false
        if (stepIndex != other.stepIndex) return false
        if (type != other.type) return false
        if (compressedValue != null) {
            if (other.compressedValue == null) return false
            if (!compressedValue.contentEquals(other.compressedValue)) return false
        } else if (other.compressedValue != null) return false
        if (tool != other.tool) return false
        if (command != other.command) return false

        return true
    }

    override fun hashCode(): Int {
        var result = conversationId.hashCode()
        result = 31 * result + stepIndex
        result = 31 * result + type.hashCode()
        result = 31 * result + (compressedValue?.contentHashCode() ?: 0)
        result = 31 * result + (tool?.hashCode() ?: 0)
        result = 31 * result + (command?.hashCode() ?: 0)
        return result
    }
}

fun TrajectoryStep.toEntity(conversationId: String, index: Int): StepEntity {
    val compressed = this.value?.let { GzipUtils.compress(it) }
    return StepEntity(
        conversationId = conversationId,
        stepIndex = index,
        type = this.case,
        compressedValue = compressed,
        tool = this.tool,
        command = this.command
    )
}
