package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.MilkEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DataExporter {

    fun exportToCsv(context: Context, entries: List<MilkEntry>): File? {
        return try {
            val cacheFile = File(context.cacheDir, "Milk_Diary_Report_${System.currentTimeMillis()}.csv")
            cacheFile.printWriter().use { writer ->
                // CSV Header
                writer.println("Date,Session,Quantity (Liters),Rate (Rs/Liter),Total Amount (Rs),Notes")
                for (entry in entries) {
                    val cleanNotes = entry.notes.replace(",", ";").replace("\n", " ")
                    writer.println("${entry.dateString},${entry.session},${entry.quantity},${entry.rate},${entry.totalAmount},\"$cleanNotes\"")
                }
            }
            cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(
        context: Context,
        entries: List<MilkEntry>,
        totalMilk: Double,
        totalEarnings: Double
    ): File? {
        return try {
            val cacheFile = File(context.cacheDir, "Milk_Diary_Report_${System.currentTimeMillis()}.pdf")
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595 x 842 PostScript points)
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val brandGreen = Color.rgb(22, 163, 74) // Green Primary
            val accentBlue = Color.rgb(37, 99, 235)  // Blue Secondary

            val titlePaint = Paint().apply {
                color = brandGreen
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                color = accentBlue
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val cellPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }

            val dividerPaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var yPos = 50f

            // App Brand Banner
            canvas.drawText("MILK DIARY PRO", 40f, yPos, titlePaint)
            yPos += 20f
            val formattedDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Generated on: $formattedDate (Offline Statement)", 40f, yPos, subtitlePaint)
            yPos += 35f

            // Summary Widgets Setup
            canvas.drawText("Production & Earnings Summary", 40f, yPos, headerPaint)
            yPos += 8f
            canvas.drawLine(40f, yPos, 555f, yPos, dividerPaint)
            yPos += 22f

            canvas.drawText("Total Transactions:  ${entries.size}", 50f, yPos, cellPaint)
            yPos += 18f
            canvas.drawText("Total Milk Quantity: ${String.format(Locale.getDefault(), "%.2f", totalMilk)} L", 50f, yPos, cellPaint)
            yPos += 18f
            canvas.drawText("Total Earned Gross:  ₹${String.format(Locale.getDefault(), "%.2f", totalEarnings)}", 50f, yPos, cellPaint)
            yPos += 35f

            // Main Ledger Table Header
            canvas.drawText("Ledger Table (Recent Actions)", 40f, yPos, headerPaint)
            yPos += 20f

            canvas.drawText("Date", 40f, yPos, tableHeaderPaint)
            canvas.drawText("Session", 150f, yPos, tableHeaderPaint)
            canvas.drawText("Qty (L)", 260f, yPos, tableHeaderPaint)
            canvas.drawText("Rate (₹/L)", 360f, yPos, tableHeaderPaint)
            canvas.drawText("Total (₹)", 460f, yPos, tableHeaderPaint)

            yPos += 6f
            canvas.drawLine(40f, yPos, 555f, yPos, dividerPaint)
            yPos += 18f

            // Ledger Rows of entries (Limit list of details to 25 items so it stays clearly on standard A4)
            val printList = entries.take(25)
            for (entry in printList) {
                canvas.drawText(entry.dateString, 40f, yPos, cellPaint)
                canvas.drawText(entry.session, 150f, yPos, cellPaint)
                canvas.drawText(String.format(Locale.getDefault(), "%.1f L", entry.quantity), 260f, yPos, cellPaint)
                canvas.drawText(String.format(Locale.getDefault(), "₹%.2f", entry.rate), 360f, yPos, cellPaint)
                canvas.drawText(String.format(Locale.getDefault(), "₹%.2f", entry.totalAmount), 460f, yPos, cellPaint)
                yPos += 18f

                if (yPos > 790f) {
                    break
                }
            }

            if (entries.size > 25) {
                yPos += 10f
                canvas.drawText("... and ${entries.size - 25} more entries generated in the CSV export report.", 40f, yPos, subtitlePaint)
            }

            pdfDoc.finishPage(page)
            FileOutputStream(cacheFile).use { fos ->
                pdfDoc.writeTo(fos)
            }
            pdfDoc.close()
            cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun backupToJson(context: Context, entries: List<MilkEntry>): File? {
        return try {
            val cacheFile = File(context.cacheDir, "Milk_Diary_Backup_${System.currentTimeMillis()}.json")
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, MilkEntry::class.java)
            val adapter = moshi.adapter<List<MilkEntry>>(listType)
            val jsonString = adapter.toJson(entries)
            cacheFile.writeText(jsonString)
            cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restoreFromJson(jsonString: String): List<MilkEntry>? {
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = Types.newParameterizedType(List::class.java, MilkEntry::class.java)
            val adapter = moshi.adapter<List<MilkEntry>>(listType)
            adapter.fromJson(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, messageTitle: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.milkdiary.gfhjqk.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, messageTitle))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
