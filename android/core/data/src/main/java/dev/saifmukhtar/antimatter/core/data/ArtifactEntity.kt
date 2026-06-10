package dev.saifmukhtar.antimatter.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "artifacts",
    primaryKeys = ["conversationId", "path"],
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
data class ArtifactEntity(
    val conversationId: String,
    val path: String,
    val name: String,
    val compressedContent: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArtifactEntity

        if (conversationId != other.conversationId) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (compressedContent != null) {
            if (other.compressedContent == null) return false
            if (!compressedContent.contentEquals(other.compressedContent)) return false
        } else if (other.compressedContent != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = conversationId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (compressedContent?.contentHashCode() ?: 0)
        return result
    }
}
