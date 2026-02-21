package com.notbraker.tracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notbraker.tracker.data.dao.HabitDao
import com.notbraker.tracker.data.model.Habit
import com.notbraker.tracker.data.model.HabitCompletion
import com.notbraker.tracker.data.model.Routine
import com.notbraker.tracker.data.model.RoutineHabitCrossRef

@Database(
    entities = [Habit::class, HabitCompletion::class, Routine::class, RoutineHabitCrossRef::class],
    version = 3,
    exportSchema = true
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        private const val DB_NAME = "habit_tracker.db"
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habits ADD COLUMN frequencyType TEXT NOT NULL DEFAULT 'DAILY'")
                database.execSQL("ALTER TABLE habits ADD COLUMN weeklyTarget INTEGER NOT NULL DEFAULT 7")
                database.execSQL("ALTER TABLE habits ADD COLUMN dailyTarget INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE habits ADD COLUMN scheduledDays TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE habits ADD COLUMN timesPerDayTarget INTEGER NOT NULL DEFAULT 1")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_habit_cross_ref (
                        routineId INTEGER NOT NULL,
                        habitId INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        PRIMARY KEY(routineId, habitId),
                        FOREIGN KEY(routineId) REFERENCES routines(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_routine_habit_cross_ref_routineId ON routine_habit_cross_ref(routineId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_routine_habit_cross_ref_habitId ON routine_habit_cross_ref(habitId)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habits ADD COLUMN originType TEXT NOT NULL DEFAULT 'CUSTOM'")
                database.execSQL("ALTER TABLE habits ADD COLUMN templateTag TEXT")
            }
        }

        @Volatile
        private var instance: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = HabitDatabase::class.java,
                    name = DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
