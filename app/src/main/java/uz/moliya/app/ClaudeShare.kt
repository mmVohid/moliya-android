package uz.moliya.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ClaudeShare {
    private const val PROMPT =
        "Mana mening moliyaviy ma'lumotlarim (Excel). Iltimos, tahlil qil: 1) xarajat odatlarimdagi asosiy muammolar, " +
            "2) jamg'arma va qarzlarim bo'yicha holat, 3) keyingi oy uchun 3 ta aniq, bajarsa bo'ladigan maslahat. " +
            "Javobni o'zbek tilida, qisqa va aniq yoz."

    /** Excel faylni vaqtinchalik yaratib, ulashish oynasini ochadi. Xato bo'lsa xabar matnini qaytaradi. */
    fun share(context: Context, data: FinanceData): String? {
        return try {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "moliya_${Dates.iso(System.currentTimeMillis())}.xlsx")
            file.outputStream().use { XlsxExporter.write(data, it) }
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, PROMPT)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Claude'ga yuborish").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        } catch (e: Exception) {
            e.message ?: "Yuborib bo'lmadi"
        }
    }
}
