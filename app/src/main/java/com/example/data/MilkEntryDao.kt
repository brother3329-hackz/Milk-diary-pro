package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkEntryDao {
    @Query("SELECT * FROM milk_entries ORDER BY dateString DESC, id DESC")
    fun getAllEntries(): Flow<List<MilkEntry>>

    @Query("SELECT * FROM milk_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): MilkEntry?

    @Query("SELECT * FROM milk_entries WHERE dateString LIKE :monthPrefix || '%' ORDER BY dateString ASC")
    fun getEntriesByMonth(monthPrefix: String): Flow<List<MilkEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MilkEntry)

    @Update
    suspend fun updateEntry(entry: MilkEntry)

    @Delete
    suspend fun deleteEntry(entry: MilkEntry)

    @Query("DELETE FROM milk_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT SUM(quantity) FROM milk_entries")
    fun getTotalMilkQuantityFlow(): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM milk_entries")
    fun getTotalEarningsFlow(): Flow<Double?>
}
