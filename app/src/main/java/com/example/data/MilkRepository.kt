package com.example.data

import kotlinx.coroutines.flow.Flow

class MilkRepository(private val milkEntryDao: MilkEntryDao) {
    val allEntries: Flow<List<MilkEntry>> = milkEntryDao.getAllEntries()
    val totalMilk: Flow<Double?> = milkEntryDao.getTotalMilkQuantityFlow()
    val totalEarnings: Flow<Double?> = milkEntryDao.getTotalEarningsFlow()

    suspend fun getEntryById(id: Long): MilkEntry? {
        return milkEntryDao.getEntryById(id)
    }

    fun getEntriesByMonth(monthPrefix: String): Flow<List<MilkEntry>> {
        return milkEntryDao.getEntriesByMonth(monthPrefix)
    }

    suspend fun insert(entry: MilkEntry) {
        milkEntryDao.insertEntry(entry)
    }

    suspend fun update(entry: MilkEntry) {
        milkEntryDao.updateEntry(entry)
    }

    suspend fun delete(entry: MilkEntry) {
        milkEntryDao.deleteEntry(entry)
    }

    suspend fun deleteById(id: Long) {
        milkEntryDao.deleteEntryById(id)
    }
}
