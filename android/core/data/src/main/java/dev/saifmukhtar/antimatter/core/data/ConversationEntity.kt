package dev.saifmukhtar.antimatter.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val stepCount: Int = 0
)
