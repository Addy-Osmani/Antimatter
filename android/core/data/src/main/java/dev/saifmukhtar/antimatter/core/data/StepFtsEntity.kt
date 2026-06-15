package dev.saifmukhtar.antimatter.core.data

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "steps_fts")
@Fts4(contentEntity = StepEntity::class)
data class StepFtsEntity(
    val conversationId: String,
    val stepIndex: Int,
    val type: String,
    val value: String?,
    val tool: String?,
    val command: String?
)
