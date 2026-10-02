package com.example.bookmarks.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Folder::class, Bookmark::class],
    version = 4,
    autoMigrations = [
        // v2: folder color + link preview columns.
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "bookmarks.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}

/**
 * v3: same tables. Previews are fetched again with the new crawler identities
 * (better results on Instagram, shops…).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE bookmarks SET description = NULL, imageUrl = NULL, siteName = NULL, " +
                "faviconUrl = NULL, previewFetchedAt = NULL",
        )
    }
}

/** v4: same tables. Preview images are no longer used: forget the stored ones. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE bookmarks SET imageUrl = NULL")
    }
}
