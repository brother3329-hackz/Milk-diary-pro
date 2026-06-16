package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MilkDiaryApplication
import com.example.data.MilkEntry
import com.example.data.MilkDiaryPreferences
import com.example.data.MilkRepository
import com.example.util.DataExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MilkViewModel(
    application: Application,
    private val repository: MilkRepository
) : AndroidViewModel(application) {

    val prefs = MilkDiaryPreferences(application)

    // Form States
    var formDate by mutableStateOf("")
    var formSession by mutableStateOf("Morning") // "Morning" (Subah) or "Evening" (Shaam)
    var formQuantity by mutableStateOf("")
    var formRate by mutableStateOf("")
    var formNotes by mutableStateOf("")
    var isEditMode by mutableStateOf(false)
    var editingEntryId by mutableStateOf<Long?>(null)

    // Settings States
    var settingsDefaultRate by mutableStateOf(prefs.defaultRate.toString())
    var settingsFeedCost by mutableStateOf(prefs.dailyFeedCost.toString())
    var settingsOtherCost by mutableStateOf(prefs.otherDailyCost.toString())

    // Validation/Message states
    var uiMessage by mutableStateOf<String?>(null)

    // Entries state
    val allEntries: StateFlow<List<MilkEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterSession = MutableStateFlow("All") // "All", "Morning", "Evening"
    val filterSession = _filterSession.asStateFlow()

    private val _filterMonth = MutableStateFlow("All") // "All", "YYYY-MM"
    val filterMonth = _filterMonth.asStateFlow()

    // Filtered Entries
    val filteredEntries: StateFlow<List<MilkEntry>> = combine(
        allEntries,
        _searchQuery,
        _filterSession,
        _filterMonth
    ) { entries, search, session, month ->
        entries.filter { entry ->
            val matchesSearch = entry.dateString.contains(search) || entry.notes.contains(search, ignoreCase = true)
            val matchesSession = session == "All" || entry.session == session
            val matchesMonth = month == "All" || entry.dateString.startsWith(month)
            matchesSearch && matchesSession && matchesMonth
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated Statistics Metrics
    val stats = allEntries.map { entries ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val todayEntries = entries.filter { it.dateString == todayStr }
        val monthEntries = entries.filter { it.dateString.startsWith(currentMonthPrefix) }

        val todayMilk = todayEntries.sumOf { it.quantity }
        val todayEarnings = todayEntries.sumOf { it.totalAmount }

        val monthMilk = monthEntries.sumOf { it.quantity }
        val monthEarnings = monthEntries.sumOf { it.totalAmount }

        // Additional stats
        val totalMorningMilk = entries.filter { it.session == "Morning" }.sumOf { it.quantity }
        val totalEveningMilk = entries.filter { it.session == "Evening" }.sumOf { it.quantity }
        val grandTotalMilk = entries.sumOf { it.quantity }
        val grandTotalEarnings = entries.sumOf { it.totalAmount }

        // Analytics metrics
        val bestEarningDay = entries.groupBy { it.dateString }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.totalAmount } }
            .maxByOrNull { it.value }
        
        val highestMilkDay = entries.groupBy { it.dateString }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.quantity } }
            .maxByOrNull { it.value }

        MilkStats(
            todayMilk = todayMilk,
            todayEarnings = todayEarnings,
            monthMilk = monthMilk,
            monthEarnings = monthEarnings,
            totalMorningMilk = totalMorningMilk,
            totalEveningMilk = totalEveningMilk,
            grandTotalMilk = grandTotalMilk,
            grandTotalEarnings = grandTotalEarnings,
            bestEarningDate = bestEarningDay?.key ?: "N/A",
            bestEarningAmount = bestEarningDay?.value ?: 0.0,
            highestMilkDate = highestMilkDay?.key ?: "N/A",
            highestMilkQty = highestMilkDay?.value ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MilkStats())

    init {
        resetForm()
    }

    fun resetForm() {
        formDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        formSession = "Morning"
        formQuantity = ""
        formRate = prefs.defaultRate.toString()
        formNotes = ""
        isEditMode = false
        editingEntryId = null
    }

    fun loadEntryToForm(entry: MilkEntry) {
        formDate = entry.dateString
        formSession = entry.session
        formQuantity = entry.quantity.toString()
        formRate = entry.rate.toString()
        formNotes = entry.notes
        isEditMode = true
        editingEntryId = entry.id
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterSession(session: String) {
        _filterSession.value = session
    }

    fun updateFilterMonth(month: String) {
        _filterMonth.value = month
    }

    // Save milk record
    fun saveEntry(onCompleted: () -> Unit) {
        val qty = formQuantity.toDoubleOrNull()
        val rt = formRate.toDoubleOrNull()

        if (formDate.isBlank()) {
            uiMessage = "Please select a valid date."
            return
        }
        if (qty == null || qty <= 0.0) {
            uiMessage = "Please enter milk quantity in Liters (greater than 0)."
            return
        }
        if (rt == null || rt <= 0.0) {
            uiMessage = "Please enter milk rate per Liter in Rupees."
            return
        }

        val calculatedTotal = qty * rt
        val entry = MilkEntry(
            id = if (isEditMode) editingEntryId ?: 0L else 0L,
            dateString = formDate,
            timestamp = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(formDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            },
            session = formSession,
            quantity = qty,
            rate = rt,
            totalAmount = calculatedTotal,
            notes = formNotes
        )

        viewModelScope.launch {
            if (isEditMode) {
                repository.update(entry)
                uiMessage = "Entry updated successfully!"
            } else {
                repository.insert(entry)
                uiMessage = "Entry saved successfully!"
            }
            resetForm()
            onCompleted()
        }
    }

    fun deleteEntry(entry: MilkEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            uiMessage = "Entry deleted successfully."
        }
    }

    // Settings management
    fun saveSettings() {
        val rate = settingsDefaultRate.toFloatOrNull()
        val feed = settingsFeedCost.toFloatOrNull()
        val other = settingsOtherCost.toFloatOrNull()

        if (rate != null) prefs.defaultRate = rate
        if (feed != null) prefs.dailyFeedCost = feed
        if (other != null) prefs.otherDailyCost = other

        uiMessage = "Settings saved successfully."
    }

    // Import Backup
    fun restoreBackup(context: Context, jsonString: String) {
        viewModelScope.launch {
            val restoredList = DataExporter.restoreFromJson(jsonString)
            if (restoredList != null && restoredList.isNotEmpty()) {
                var importedCount = 0
                for (entry in restoredList) {
                    repository.insert(entry.copy(id = 0L)) // Insert as new clean record
                    importedCount++
                }
                uiMessage = "$importedCount entries restored successfully!"
            } else {
                uiMessage = "Error: Invalid backup file format."
            }
        }
    }

    // Clear message
    fun clearMessage() {
        uiMessage = null
    }
}

// Stats metrics class
data class MilkStats(
    val todayMilk: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val monthMilk: Double = 0.0,
    val monthEarnings: Double = 0.0,
    val totalMorningMilk: Double = 0.0,
    val totalEveningMilk: Double = 0.0,
    val grandTotalMilk: Double = 0.0,
    val grandTotalEarnings: Double = 0.0,
    val bestEarningDate: String = "N/A",
    val bestEarningAmount: Double = 0.0,
    val highestMilkDate: String = "N/A",
    val highestMilkQty: Double = 0.0
)

// Factory implementation for ViewModel
class ViewModelFactory(
    private val application: Application,
    private val repository: MilkRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MilkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MilkViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
