package com.notbraker.tracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.notbraker.tracker.data.dao.HabitDao
import com.notbraker.tracker.data.model.Habit
import com.notbraker.tracker.data.model.HabitCompletion

@Database(
    entities = [Habit::class, HabitCompletion::class],
    version = 1,
    exportSchema = true
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        private const val DB_NAME = "habit_tracker.db"

        @Volatile
        private var instance: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = HabitDatabase::class.java,
                    name = DB_NAME
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
