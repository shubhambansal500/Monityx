package com.bansalcoders.monityx.domain.usecase

import android.content.Context
import com.bansalcoders.monityx.domain.model.Subscription
import com.bansalcoders.monityx.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Exports all subscriptions to CSV or plain-text PDF.
 * Uses only Android SDK APIs – no external PDF library required.
 */
class ExportDataUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    @ApplicationContext private val context: Context,
) {
    enum class Format { CSV, PDF }

    sealed class Result {
        data class Success(val file: File) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(format: Format): Result {
        return try {
            val subscriptions = repository.getAllSubscriptions().first()
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "subscriptions_$timestamp.${format.name.lowercase()}"
            val exportDir = File(context.getExternalFilesDir(null), "exports").also { it.mkdirs() }
            val file = File(exportDir, fileName)

            when (format) {
                Format.CSV -> writeCsv(file, subscriptions)
                Format.PDF -> writePdf(file, subscriptions)
            }

            Result.Success(file)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Export failed")
        }
    }

    // ── CSV ───────────────────────────────────────────────────────────────────

    private fun writeCsv(file: File, subscriptions: List<Subscription>) {
        val header = "Name,Category,Cost,Currency,Billing Cycle,Start Date,Next Billing,Monthly Cost,Active\n"
        val rows = subscriptions.joinToString("\n") { s ->
            listOf(
                s.name.csvEscape(),
                s.category.label,
                "%.2f".format(s.cost),
                s.currency,
                s.billingCycle.label,
                s.startDate.toString(),
                s.nextBillingDate.toString(),
                "%.2f".format(s.monthlyCost),
                s.isActive.toString(),
            ).joinToString(",")
        }
        file.writeText(header + rows, Charsets.UTF_8)
    }

    private fun String.csvEscape(): String =
        if (contains(',') || contains('"') || contains('\n')) "\"${replace("\"", "\"\"")}\"" else this

    // ── PDF (Android PdfDocument) ─────────────────────────────────────────────

    private fun writePdf(file: File, subscriptions: List<Subscription>) {
        val document = android.graphics.pdf.PdfDocument()
        val paint = android.graphics.Paint().apply { textSize = 12f; isAntiAlias = true }
        val titlePaint = android.graphics.Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val pageWidth = 595
        val pageHeight = 842  // A4
        val margin = 40f
        var pageNumber = 1
        var y = margin + 30f

        fun newPage(): android.graphics.pdf.PdfDocument.Page {
            val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            return document.startPage(info)
        }

        var currentPage = newPage()
        var canvas = currentPage.canvas

        // Title
        canvas.drawText("Subscription Manager – Export", margin, y, titlePaint)
        y += 30f
        canvas.drawText("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))}", margin, y, paint)
        y += 40f

        subscriptions.forEach { sub ->
            if (y > pageHeight - margin) {
                document.finishPage(currentPage)
                currentPage = newPage()
                canvas = currentPage.canvas
                y = margin + 30f
            }
            canvas.drawText("• ${sub.name}  |  ${sub.category.label}  |  ${sub.currency} ${"%.2f".format(sub.cost)} / ${sub.billingCycle.label}", margin, y, paint)
            y += 18f
            canvas.drawText("   Next billing: ${sub.nextBillingDate}  |  Monthly equiv: ${"%.2f".format(sub.monthlyCost)}", margin, y, paint)
            y += 24f
        }

        document.finishPage(currentPage)
        file.outputStream().use { document.writeTo(it) }
        document.close()
    }
}
