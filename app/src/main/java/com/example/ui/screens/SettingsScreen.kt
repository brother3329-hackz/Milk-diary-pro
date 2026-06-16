package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MilkViewModel
import com.example.util.DataExporter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val entries by viewModel.allEntries.collectAsState()
    val stats by viewModel.stats.collectAsState()

    // Setup JSON File Restorer Launcher
    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonText = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                }
                if (jsonText != null) {
                    viewModel.restoreBackup(context, jsonText)
                } else {
                    Toast.makeText(context, "Failed to read backup file.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error reading backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ledger Settings & Utilities", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Default rates pre-fills & maintenance costs config
            Text(
                text = "Default Configurations (Farmer Settings)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = viewModel.settingsDefaultRate,
                onValueChange = { viewModel.settingsDefaultRate = it },
                label = { Text("Default Rate (₹ per Liter)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_default_rate_input"),
                leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) }
            )

            OutlinedTextField(
                value = viewModel.settingsFeedCost,
                onValueChange = { viewModel.settingsFeedCost = it },
                label = { Text("Daily Feed Expense Cost (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_feed_cost_input"),
                leadingIcon = { Icon(Icons.Default.Agriculture, contentDescription = null) }
            )

            OutlinedTextField(
                value = viewModel.settingsOtherCost,
                onValueChange = { viewModel.settingsOtherCost = it },
                label = { Text("Daily Other Expenses - Electricity/Labor (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_utility_cost_input"),
                leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null) }
            )

            Button(
                onClick = {
                    viewModel.saveSettings()
                    Toast.makeText(context, "Configurations saved locally!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings Configuration", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(6.dp))

            // Section 2: Backup and Document Export Section
            Text(
                text = "Export Documents & Ledger Reports",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Export to PDF button
            Card(
                onClick = {
                    if (entries.isEmpty()) {
                        Toast.makeText(context, "Cannot generate report: No transactions logged.", Toast.LENGTH_SHORT).show()
                    } else {
                        val pdfFile = DataExporter.exportToPdf(
                            context = context,
                            entries = entries,
                            totalMilk = stats.grandTotalMilk,
                            totalEarnings = stats.grandTotalEarnings
                        )
                        if (pdfFile != null) {
                            DataExporter.shareFile(
                                context = context,
                                file = pdfFile,
                                mimeType = "application/pdf",
                                messageTitle = "Share Milk Diary Statement PDF"
                            )
                        } else {
                            Toast.makeText(context, "Failed to compile PDF document.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_pdf_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Export PDF Report", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Generates a beautiful offline printable dairy statement.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            // Export to CSV Excel button
            Card(
                onClick = {
                    if (entries.isEmpty()) {
                        Toast.makeText(context, "Cannot export CSV: Ledger is empty.", Toast.LENGTH_SHORT).show()
                    } else {
                        val csvFile = DataExporter.exportToCsv(
                            context = context,
                            entries = entries
                        )
                        if (csvFile != null) {
                            DataExporter.shareFile(
                                context = context,
                                file = csvFile,
                                mimeType = "text/csv",
                                messageTitle = "Share Milk Diary CSV (Excel Spreadsheet)"
                            )
                        } else {
                            Toast.makeText(context, "Failed to compile Excel CSV workbook.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_excel_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Excel",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Export Excel Spreadsheet (CSV)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Detailed spreadsheet database compatible with Microsoft Excel.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            // Quick Formatted Text Share (Perfect for SMS / WhatsApp copy pasting)
            Card(
                onClick = {
                    if (entries.isEmpty()) {
                        Toast.makeText(context, "No transactions to share.", Toast.LENGTH_SHORT).show()
                    } else {
                        val shareMessage = """
                            *🥛 MILK DIARY PRO SUMMARY STATEMENT*
                            ------------------------------------
                            Total Recorded Deliveries: ${entries.size} slips
                            Total Milk Quantity Delivered: ${String.format(Locale.getDefault(), "%.1f", stats.grandTotalMilk)} Liters
                            Aggregate Gross Earnings: ₹${String.format(Locale.getDefault(), "%.2f", stats.grandTotalEarnings)}
                            ------------------------------------
                            *Keep track of milk deliveries offline with no papers!*
                        """.trimIndent()

                        val textIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(android.content.Intent.createChooser(textIntent, "Share Ledger Summary via"))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_statement_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share text",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Quick Share Summary Message", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Send a formatted statement summary over WhatsApp or SMS.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(6.dp))

            // Section 3: Backup & Restoration
            Text(
                text = "Database Backup & Restoration",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Generate secure backup JSON
                Button(
                    onClick = {
                        if (entries.isEmpty()) {
                            Toast.makeText(context, "Cannot generate backup: Database is empty.", Toast.LENGTH_SHORT).show()
                        } else {
                            val backupFile = DataExporter.backupToJson(context, entries)
                            if (backupFile != null) {
                                DataExporter.shareFile(
                                    context = context,
                                    file = backupFile,
                                    mimeType = "application/json",
                                    messageTitle = "Share Secure Milk Diary Backup"
                                )
                            } else {
                                Toast.makeText(context, "Backup failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("generate_backup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Restore backup json
                Button(
                    onClick = {
                        jsonRestoreLauncher.launch("application/json")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("restore_backup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
