package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MilkEntry
import com.example.ui.viewmodel.MilkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: MilkViewModel,
    onNavigateToEdit: (MilkEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.allEntries.collectAsState()
    val filteredEntries by viewModel.filteredEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterSession by viewModel.filterSession.collectAsState()
    val filterMonth by viewModel.filterMonth.collectAsState()

    var showDeleteConfirmId by remember { mutableStateOf<Long?>(null) }

    // Dynamic month calculations from data list
    val uniqueMonths = remember(entries) {
        entries.map { it.dateString.take(7) } // e.g. "2026-06"
            .distinct()
            .sortedDescending()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Milk Dairy Ledger",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search by Date (YYYY-MM-DD) or notes...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("ledger_search_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Session filter segment
                Text("Filter by Session", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Morning", "Evening").forEach { s ->
                        val selected = filterSession == s
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateFilterSession(s) },
                            label = { Text(if (s == "All") "All Sessions" else if (s == "Morning") "Morning (Subah)" else "Evening (Shaam)") },
                            modifier = Modifier.testTag("filter_chip_$s")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic Months Filters horizontal row
                if (uniqueMonths.isNotEmpty()) {
                    Text("Filter by Month", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = filterMonth == "All",
                                onClick = { viewModel.updateFilterMonth("All") },
                                label = { Text("All Months") }
                            )
                        }
                        items(uniqueMonths) { monthCode ->
                            // Format title "2026-06" to "June 2026"
                            val displayMonth = try {
                                val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthCode)
                                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(parsed ?: Date())
                            } catch (e: Exception) {
                                monthCode
                            }

                            FilterChip(
                                selected = filterMonth == monthCode,
                                onClick = { viewModel.updateFilterMonth(monthCode) },
                                label = { Text(displayMonth) }
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredEntries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recorded transactions found matching those search metrics.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        LedgerRowCard(
                            entry = entry,
                            onEdit = { onNavigateToEdit(entry) },
                            onDeleteTriggered = { showDeleteConfirmId = entry.id }
                        )
                    }
                }
            }

            // Delete Confirmation Pop-up overlay
            showDeleteConfirmId?.let { idToDelete ->
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmId = null },
                    title = { Text("Delete Slip Record") },
                    text = { Text("Are you absolutely sure you want to permanently delete this milk transaction? This cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.allEntries.value.find { it.id == idToDelete }?.let { entry ->
                                    viewModel.deleteEntry(entry)
                                }
                                showDeleteConfirmId = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmId = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LedgerRowCard(
    entry: MilkEntry,
    onEdit: () -> Unit,
    onDeleteTriggered: () -> Unit
) {
    val dateDisplay = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.dateString)
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(parsed ?: Date())
    } catch (e: Exception) {
        entry.dateString
    }

    val isMorning = entry.session == "Morning"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_item_${entry.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row of single item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMorning) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMorning) Icons.Default.WbSunny else Icons.Default.ModeNight,
                            contentDescription = "Session",
                            tint = if (isMorning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateDisplay,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Entry",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteTriggered,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Entry",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body containing details columns: Liters, Rate, Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Session", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(if (isMorning) "Morning (Subah)" else "Evening (Shaam)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Quantity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("${entry.quantity} Liters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹${entry.rate}/L", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Gross", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹${String.format(Locale.getDefault(), "%.2f", entry.totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }

            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Notes",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
