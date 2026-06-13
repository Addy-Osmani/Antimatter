package dev.saifmukhtar.antimatter.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [ConversationEntity::class, StepEntity::class, ArtifactEntity::class],
    version = 2,
    // exportSchema = true so Room generates schema JSON files for migration tracking.
    // Add the schema export directory to build.gradle.kts:
    //   ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                // addMigrations: add explicit MIGRATION_X_Y objects here as the schema evolves.
                // Never use fallbackToDestructiveMigration() in production — it silently
                // drops all user data on schema mismatch.
                // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
