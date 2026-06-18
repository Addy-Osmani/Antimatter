package dev.saifmukhtar.antimatter.core.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideUserPrefs(@ApplicationContext ctx: Context): UserPreferencesRepository =
        UserPreferencesRepository(ctx)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext ctx: Context,
        userPrefs: UserPreferencesRepository
    ): AppDatabase = AppDatabase.getDatabase(ctx, userPrefs.getDatabasePassphrase())

    @Provides
    fun provideAppDao(db: AppDatabase): AppDao = db.appDao()
}
