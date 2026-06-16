package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MilkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryScreen(
    viewModel: MilkViewModel,
    onCompleted: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Calculated derived amount
    val qtyDouble = viewModel.formQuantity.toDoubleOrNull() ?: 0.0
    val rateDouble = viewModel.formRate.toDoubleOrNull() ?: 0.0
    val calculatedAmount = qtyDouble * rateDouble

    // Local DatePickerDialog parameters setup
    val calendar = Calendar.getInstance()
    if (viewModel.formDate.isNotEmpty()) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(viewModel.formDate)
            if (date != null) {
                calendar.time = date
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            val monthFormatted = if (m + 1 < 10) "0${m + 1}" else "${m + 1}"
            val dayFormatted = if (d < 10) "0$d" else "$d"
            viewModel.formDate = "$y-$monthFormatted-$dayFormatted"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditMode) "Edit Milk Entry" else "Add Milk Delivery Slip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
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
            // General Form Info
            Text(
                text = "Enter transaction details carefully. This will automatically update your journal ledger.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // 1. Date Field Picker
            Text(
                text = "Date Selection",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Card(
                onClick = { datePickerDialog.show() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_picker_trigger"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.formDate,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Change Date",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 2. Session Selector (सुबह vs शाम)
            Text(
                text = "Session (समय)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sessions = listOf("Morning" to "सुबह", "Evening" to "शाम")
                sessions.forEach { (sessionKey, label) ->
                    val isSelected = viewModel.formSession == sessionKey
                    val color = if (sessionKey == "Morning") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                    Button(
                        onClick = { viewModel.formSession = sessionKey },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("session_choice_$sessionKey"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                    ) {
                        Icon(
                            imageVector = if (sessionKey == "Morning") Icons.Default.WbSunny else Icons.Default.ModeNight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$sessionKey ($label)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Milk Quantity (Liters)
            Text(
                text = "Milk Quantity (Liters)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = viewModel.formQuantity,
                onValueChange = { viewModel.formQuantity = it },
                label = { Text("Liters (e.g. 10.5)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_quantity_input"),
                leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )

            // 4. Rate per Liter (Rupees)
            Text(
                text = "Rate per Liter (₹)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = viewModel.formRate,
                onValueChange = { viewModel.formRate = it },
                label = { Text("Rate in ₹/L (e.g. 45.00)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_rate_input"),
                leadingIcon = { Icon(Icons.Default.Money, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )

            // Realtime Auto Calculated Earnings block
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto Calculated Total",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Quantity × Rate per liter",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "₹%.2f", calculatedAmount),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("calculated_total_display")
                    )
                }
            }

            // 5. Notes Option
            Text(
                text = "Notes (Optional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = viewModel.formNotes,
                onValueChange = { viewModel.formNotes = it },
                label = { Text("Extra remarks (e.g. Buffalo, High fat...)") },
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_notes_input"),
                leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.outline) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save Slips Button block
            Button(
                onClick = {
                    viewModel.saveEntry(onCompleted = onCompleted)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_slip_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (viewModel.isEditMode) "Update Transaction Slip" else "Save Delivery Slip",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
