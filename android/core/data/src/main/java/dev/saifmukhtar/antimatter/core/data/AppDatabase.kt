package dev.saifmukhtar.antimatter.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [ConversationEntity::class, StepEntity::class, ArtifactEntity::class, AgentEntity::class, StepFtsEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create agents table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `agents` (
                        `id` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `lastSeen` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // Add agentId to conversations
                db.execSQL("ALTER TABLE conversations ADD COLUMN agentId TEXT NOT NULL DEFAULT 'legacy'")
            }
        }

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
                
                // Populate the FTS table
                db.execSQL("INSERT INTO steps_fts(steps_fts) VALUES ('rebuild')")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val userPrefs = UserPreferencesRepository(context)
                val passphrase = userPrefs.getDatabasePassphrase()
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "antimatter_database"
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
