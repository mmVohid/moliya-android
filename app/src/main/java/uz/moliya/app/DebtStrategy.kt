package uz.moliya.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

data class PayoffStep(val name: String, val month: Int)
data class PayoffResult(val steps: List<PayoffStep>, val months: Int, val interest: Long, val feasible: Boolean)

object DebtStrategy {
    /** method 0 = qor ko'chkisi (kichik qoldiqdan), 1 = ko'chki (yuqori foizdan). Foiz yillik deb olinadi. */
    fun simulate(debts: List<Debt>, monthly: Long, method: Int): PayoffResult {
        val open = debts.filter { !it.isLent && it.remaining > 0 }
        val balance = open.associate { it.id to it.remaining.toDouble() }.toMutableMap()
        val closedAt = HashMap<Long, Int>()
        var interestTotal = 0.0
        var month = 0
        while (balance.values.any { it > 0.5 } && month < 600) {
            month++
            for (d in open) {
                val b = balance[d.id] ?: continue
                if (b > 0.5 && d.interest > 0.0) {
                    val add = b * d.interest / 1200.0
                    balance[d.id] = b + add
                    interestTotal += add
                }
            }
            var budget = monthly.toDouble()
            val order = open.filter { (balance[it.id] ?: 0.0) > 0.5 }.let { list ->
                if (method == 0) list.sortedBy { balance[it.id] } else list.sortedByDescending { it.interest }
            }
            for (d in order) {
                if (budget <= 0.0) break
                val b = balance[d.id] ?: continue
                val pay = Math.min(b, budget)
                balance[d.id] = b - pay
                budget -= pay
                if ((balance[d.id] ?: 0.0) <= 0.5 && d.id !in closedAt) closedAt[d.id] = month
            }
        }
        val feasible = balance.values.none { it > 0.5 }
        val steps = open.filter { it.id in closedAt }.sortedBy { closedAt[it.id] }.map { PayoffStep(it.name, closedAt[it.id] ?: 0) }
        return PayoffResult(steps, month, interestTotal.toLong(), feasible)
    }

    fun dateAfter(months: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.MONTH, months)
        return Dates.monthNames[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR)
    }
}

@Composable
fun DebtStrategySheet(debts: List<Debt>, onDismiss: () -> Unit) {
    var method by remember { mutableStateOf(0) }
    var monthly by remember { mutableStateOf("") }
    val value = parseMoney(monthly)
    val open = debts.filter { !it.isLent && it.remaining > 0 }
    val total = open.sumOf { it.remaining }
    val result = if (value > 0) DebtStrategy.simulate(debts, value, method) else null

    FormSheet(title = "Qarzni yopish rejasi", onDismiss = onDismiss) {
        Text(
            "Jami qoldiq: ${formatMoney(total)}. Oyiga barcha qarzlarga qancha ajrata olasiz?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyField(value = monthly, onChange = { monthly = it }, label = "Oyiga to'lov summasi")
        SegmentedToggle(
            options = listOf("Kichigidan", "Foizi yuqorisidan"),
            selected = method,
            onSelect = { method = it },
        )
        Text(
            if (method == 0) "Eng kichik qoldiqli qarz birinchi yopiladi. Tez natija beradi, motivatsiyani oshiradi."
            else "Foizi eng yuqori qarz birinchi yopiladi. Jami foiz to'lovi kamroq bo'ladi. Foiz yillik deb hisoblanadi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (result != null) {
            if (!result.feasible) {
                Text(
                    "Bu summa bilan qarzlar yopilmaydi (foiz to'lovdan ko'p). Oylik to'lovni oshiring.",
                    style = MaterialTheme.typography.titleSmall,
                    color = Brand.expense,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(
                    "Hammasi ${result.months} oyda yopiladi (${DebtStrategy.dateAfter(result.months)}).",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Brand.income,
                )
                if (result.interest > 0) {
                    Text(
                        "Jami foiz: ${formatMoney(result.interest)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                result.steps.forEachIndexed { i, step ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ${step.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("${step.month}-oy", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
