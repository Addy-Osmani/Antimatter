package dev.saifmukhtar.antimatter.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val status: String, // "online" or "offline"
    val lastSeen: Long
)
