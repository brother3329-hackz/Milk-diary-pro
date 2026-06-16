package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MilkEntry::class], version = 1, exportSchema = false)
abstract class MilkDatabase : RoomDatabase() {
    abstract val milkEntryDao: MilkEntryDao

    companion object {
        @Volatile
        private var INSTANCE: MilkDatabase? = null

        fun getDatabase(context: Context): MilkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MilkDatabase::class.java,
                    "milk_diary_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
