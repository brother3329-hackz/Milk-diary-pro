package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milk_entries")
data class MilkEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateString: String, // Format: YYYY-MM-DD
    val timestamp: Long, // Epoch millis
    val session: String, // "Morning" (Subah) or "Evening" (Shaam)
    val quantity: Double, // Liters
    val rate: Double, // Rate per liter
    val totalAmount: Double, // quantity * rate
    val notes: String = ""
)
