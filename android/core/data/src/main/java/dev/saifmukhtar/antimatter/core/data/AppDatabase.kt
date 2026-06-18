package dev.saifmukhtar.antimatter.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [ConversationEntity::class, StepEntity::class, ArtifactEntity::class, AgentEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ── Migration 1 → 2 ──────────────────────────────────────────────
        // Establishes the base schema (conversations + steps) if someone is
        // upgrading from the very first build before migrations were tracked.
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `steps` (
                        `conversationId` TEXT NOT NULL,
                        `stepIndex` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `compressedValue` BLOB,
                        `tool` TEXT,
                        `command` TEXT,
                        PRIMARY KEY(`conversationId`, `stepIndex`),
                        FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_steps_conversationId` ON `steps` (`conversationId`)")
            }
        }

        // ── Migration 2 → 3 ──────────────────────────────────────────────
        // Added: agents table + agentId column on conversations.
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `agents` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastSeen` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE conversations ADD COLUMN agentId TEXT NOT NULL DEFAULT 'legacy'")
            }
        }

        // ── Migration 3 → 4 ──────────────────────────────────────────────
        // Added: FTS4 virtual table for steps (content-based, for search).
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `steps_fts` USING FTS4(
                        `conversationId` TEXT NOT NULL,
                        `stepIndex` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `value` TEXT,
                        `tool` TEXT,
                        `command` TEXT,
                        content=`steps`
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO steps_fts(steps_fts) VALUES ('rebuild')")
            }
        }

        // ── Migration 4 → 5 ──────────────────────────────────────────────
        // Added: artifacts table + scroll-position / step-count columns on conversations.
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // New columns on conversations
                db.execSQL("ALTER TABLE conversations ADD COLUMN scrollIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN scrollOffset INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN stepCount INTEGER NOT NULL DEFAULT 0")

                // New artifacts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `artifacts` (
                        `conversationId` TEXT NOT NULL,
                        `path` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `compressedContent` BLOB,
                        PRIMARY KEY(`conversationId`, `path`),
                        FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_artifacts_conversationId` ON `artifacts` (`conversationId`)")
            }
        }

        /**
         * Returns the singleton [AppDatabase] instance.
         *
         * @param passphrase The SQLCipher database passphrase. Pass in the value from
         *   [UserPreferencesRepository.getDatabasePassphrase] via the Hilt module so that
         *   we never create a second, detached [UserPreferencesRepository] instance here.
         */
        fun getDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "antimatter_database"
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
