package uz.moliya.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun LimitsSheet(
    limits: Map<String, Long>,
    categories: List<String>,
    onSave: (Map<String, Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val fields = remember {
        mutableStateMapOf<String, String>().apply {
            categories.forEach { c -> put(c, limits[c]?.toString() ?: "") }
        }
    }

    FormSheet(title = "Oylik limitlar", onDismiss = onDismiss) {
        Text(
            "Kategoriya uchun oylik xarajat limitini qo'ying. Limit 80% dan oshsa Bosh sahifada ogohlantiramiz, oshib ketsa xabar beramiz. Bo'sh qoldirsangiz, limitsiz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        categories.forEach { category ->
            MoneyField(
                value = fields[category] ?: "",
                onChange = { fields[category] = it },
                label = category,
            )
        }
        BigButton(
            text = "Saqlash",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val result = LinkedHashMap<String, Long>()
                fields.forEach { (category, text) ->
                    val value = parseMoney(text)
                    if (value > 0L) result[category] = value
                }
                onSave(result)
                onDismiss()
            },
        )
    }
}
