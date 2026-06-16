package com.example

import android.app.Application
import com.example.data.MilkDatabase
import com.example.data.MilkRepository

class MilkDiaryApplication : Application() {
    val database by lazy { MilkDatabase.getDatabase(this) }
    val repository by lazy { MilkRepository(database.milkEntryDao) }
}
