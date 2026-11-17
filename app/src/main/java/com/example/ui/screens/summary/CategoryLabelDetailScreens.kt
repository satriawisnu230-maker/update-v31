package com.example.ui.screens.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.viewmodel.MonthOption
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyHelper
import com.example.util.TransactionMathHelper
import kotlin.math.abs

private val shortMonthLabels = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

@Composable
fun CategoryDetailScreen(
    categoryName: String,
    txType: TransactionType,
    initialMonth: Int,
    initialYear: Int,
    monthOptions: List<MonthOption>,
    allTransactions: List<Transaction>,
    selectedWallet: String,
    onTransactionClick: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    var currentMonth by remember { mutableStateOf(initialMonth) }
    var currentYear by remember { mutableStateOf(initialYear) }
    var showBarChart by remember { mutableStateOf(true) }
    var selectedSubcategory by remember { mutableStateOf<String?>(null) }

    val walletFiltered = remember(allTransactions, selectedWallet) {
        if (selectedWallet == "Semua") allTransactions
        else allTransactions.filter { it.accountName.equals(selectedWallet, ignoreCase = true) }
    }

    val monthTransactions = remember(walletFiltered, categoryName, txType, currentMonth, currentYear) {
        walletFiltered.filter {
            it.type == txType &&
                it.category.equals(categoryName, ignoreCase = true) &&
                it.monthIndex == currentMonth &&
                it.year == currentYear
        }.sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
    }

    val prevMonthPair = remember(currentMonth, currentYear) {
        if (currentMonth > 1) (currentMonth - 1) to currentYear else 12 to (currentYear - 1)
    }
    val prevMonthTransactions = remember(walletFiltered, categoryName, txType, prevMonthPair) {
        walletFiltered.filter {
            it.type == txType &&
                it.category.equals(categoryName, ignoreCase = true) &&
                it.monthIndex == prevMonthPair.first &&
                it.year == prevMonthPair.second
        }
    }
    val prevMonthTotal = remember(prevMonthTransactions) { prevMonthTransactions.sumOf { it.amount } }
    val prevSubMap = remember(prevMonthTransactions) {
        prevMonthTransactions.groupBy { it.subcategory.ifBlank { "Umum" } }
            .mapValues { e -> e.value.sumOf { it.amount } }
    }

    val totalAmount = remember(monthTransactions) { monthTransactions.sumOf { it.amount } }
    val deltaPercent = remember(totalAmount, prevMonthTotal) {
        when {
            prevMonthTotal > 0.0 -> ((totalAmount - prevMonthTotal) / prevMonthTotal) * 100.0
            totalAmount > 0.0 -> 100.0
            else -> 0.0
        }
    }

    val subcategoryBreakdown = remember(monthTransactions, totalAmount, prevSubMap) {
        monthTransactions.groupBy { it.subcategory.ifBlank { "Umum" } }
            .entries
            .map { (sub, list) ->
                val sum = list.sumOf { it.amount }
                val pct = if (totalAmount > 0.0) ((sum / totalAmount) * 100.0).toFloat() else 0f
                val prevSub = prevSubMap[sub] ?: 0.0
                val subDeltaPct = when {
                    prevSub > 0.0 -> ((sum - prevSub) / prevSub) * 100.0
                    sum > 0.0 && prevMonthTotal > 0.0 -> 100.0
                    else -> null
                }
                CategorySubBreakdownRow(sub, sum, pct, subDeltaPct)
            }
            .sortedByDescending { it.amount }
    }

    val visibleTransactions = remember(monthTransactions, selectedSubcategory) {
        val target = selectedSubcategory
        if (target == null) monthTransactions
        else monthTransactions.filter { it.subcategory.ifBlank { "Umum" } == target }
    }

    val sixMonthsTrend = remember(walletFiltered, categoryName, txType, currentMonth, currentYear) {
        (5 downTo 0).map { offset ->
            var m = currentMonth - offset
            var y = currentYear
            while (m <= 0) {
                m += 12
                y -= 1
            }
            val amt = walletFiltered.filter {
                it.type == txType &&
                    it.category.equals(categoryName, ignoreCase = true) &&
                    it.monthIndex == m &&
                    it.year == y
            }.sumOf { it.amount }
            Triple(shortMonthLabels.getOrElse(m) { "" }, amt, m == currentMonth && y == currentYear)
        }
    }

    val accentColor = CategoryIconHelper.getCategoryColor(categoryName, txType)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_back_category_detail")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = categoryName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showBarChart = !showBarChart }
                            .testTag("btn_toggle_category_chart")
                    ) {
                        Icon(
                            imageVector = if (showBarChart) Icons.Default.ShowChart else Icons.Default.BarChart,
                            contentDescription = "Ubah tampilan grafik",
                            tint = AccentBlueBright,
                            modifier = Modifier.padding(7.dp).size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated
                    ) {
                        Text(
                            text = selectedWallet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Month chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthOptions, key = { it.year * 100 + it.monthIndex }) { opt ->
                        val isSel = opt.monthIndex == currentMonth && opt.year == currentYear
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) AccentBlue else DarkSurfaceElevated,
                            modifier = Modifier.clickable {
                                currentMonth = opt.monthIndex
                                currentYear = opt.year
                                selectedSubcategory = null
                            }
                        ) {
                            Text(
                                text = opt.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Summary Hero Card + 6-Month Chart with Bar/Line Toggle
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(accentColor.copy(alpha = 0.16f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getCategoryIcon(categoryName),
                                                contentDescription = categoryName,
                                                tint = accentColor,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = CurrencyHelper.formatRupiah(totalAmount),
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                            val deltaColor = if (txType == TransactionType.EXPENSE) {
                                                if (deltaPercent <= 0) IncomeGreen else ExpenseRed
                                            } else {
                                                if (deltaPercent >= 0) IncomeGreen else ExpenseRed
                                            }
                                            val arrow = if (deltaPercent >= 0) "▲" else "▼"
                                            Text(
                                                text = "$arrow ${abs(deltaPercent).toInt()}%",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = deltaColor
                                            )
                                        }
                                    }

                                    // Comparison column: dibanding Bln Thn
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "dibanding ${shortMonthLabels.getOrElse(prevMonthPair.first) { "" }} ${prevMonthPair.second}",
                                            fontSize = 10.5.sp,
                                            color = TextSubtle
                                        )
                                        Text(
                                            text = CurrencyHelper.formatCompactRupiah(prevMonthTotal),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val nonZero6 = sixMonthsTrend.filter { it.second > 0.0 }
                                val avg6 = if (nonZero6.isNotEmpty()) nonZero6.sumOf { it.second } / nonZero6.size else 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tren 6 Bulan", fontSize = 11.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                                    Surface(shape = RoundedCornerShape(8.dp), color = DarkSurfaceElevated) {
                                        Text(
                                            text = "Rata-rata: ${CurrencyHelper.formatCompactRupiah(avg6)}/bln",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                SixMonthMiniChart(
                                    data = sixMonthsTrend,
                                    accentColor = accentColor,
                                    isBar = showBarChart
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (prevMonthTotal > 0.0) {
                                        val diffAmt = abs(totalAmount - prevMonthTotal)
                                        if (totalAmount <= prevMonthTotal) {
                                            "Lebih hemat ${CurrencyHelper.formatCompactRupiah(diffAmt)} dibanding ${shortMonthLabels.getOrElse(prevMonthPair.first) { "" }}."
                                        } else {
                                            "Naik ${CurrencyHelper.formatCompactRupiah(diffAmt)} dibanding ${shortMonthLabels.getOrElse(prevMonthPair.first) { "" }}."
                                        }
                                    } else {
                                        "Belum ada perbandingan di ${shortMonthLabels.getOrElse(prevMonthPair.first) { "" }} ${prevMonthPair.second}."
                                    },
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Subcategory Rincian Breakdown
                    if (subcategoryBreakdown.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Rincian",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        if (selectedSubcategory != null) {
                                            Text(
                                                text = "Tampilkan Semua",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AccentBlue,
                                                modifier = Modifier.clickable { selectedSubcategory = null }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    subcategoryBreakdown.forEachIndexed { idx, row ->
                                        val isSubSelected = selectedSubcategory == row.name
                                        if (idx > 0) Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSubSelected) accentColor.copy(alpha = 0.16f) else DarkSurfaceElevated.copy(alpha = 0.7f),
                                            border = if (isSubSelected) BorderStroke(1.dp, accentColor) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedSubcategory = if (isSubSelected) null else row.name
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = row.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextWhite
                                                        )
                                                        row.deltaPct?.let { d ->
                                                            val good = if (txType == TransactionType.EXPENSE) d <= 0 else d >= 0
                                                            Text(
                                                                text = "${if (d >= 0) "▲" else "▼"} ${abs(d).toInt()}% vs bln lalu",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = if (good) IncomeGreen else ExpenseRed
                                                            )
                                                        }
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = CurrencyHelper.formatRupiah(row.amount),
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextWhite
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "${row.percent.toInt()}%",
                                                            fontSize = 11.sp,
                                                            color = TextMuted
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = { (row.percent / 100f).coerceIn(0f, 1f) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = accentColor,
                                                    trackColor = DarkSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Transactions List Grouped by Day
                    item {
                        DetailTransactionsCard(
                            title = if (selectedSubcategory != null) "Transaksi · $selectedSubcategory (${visibleTransactions.size})" else "Transaksi (${visibleTransactions.size})",
                            transactions = visibleTransactions,
                            onTransactionClick = onTransactionClick
                        )
                    }
                }
            }
        }
    }
}

private data class CategorySubBreakdownRow(
    val name: String,
    val amount: Double,
    val percent: Float,
    val deltaPct: Double?
)

@Composable
fun LabelDetailScreen(
    labelName: String,
    initialMonth: Int,
    initialYear: Int,
    monthOptions: List<MonthOption>,
    allTransactions: List<Transaction>,
    selectedWallet: String,
    onTransactionClick: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    var currentMonth by remember { mutableStateOf(initialMonth) }
    var currentYear by remember { mutableStateOf(initialYear) }
    var showBarChart by remember { mutableStateOf(true) }

    val walletFiltered = remember(allTransactions, selectedWallet) {
        if (selectedWallet == "Semua") allTransactions
        else allTransactions.filter { it.accountName.equals(selectedWallet, ignoreCase = true) }
    }

    val matchingTxs = remember(walletFiltered, labelName, currentMonth, currentYear) {
        walletFiltered.filter { tx ->
            val tags = tx.label.split(",").map { it.trim() }
            val matchesLabel = tags.any { it.equals(labelName, ignoreCase = true) }
            matchesLabel && tx.monthIndex == currentMonth && tx.year == currentYear
        }.sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
    }

    val expenseTotal = remember(matchingTxs) {
        matchingTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val incomeTotal = remember(matchingTxs) {
        matchingTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    val sixMonthsTrend = remember(walletFiltered, labelName, currentMonth, currentYear) {
        (5 downTo 0).map { offset ->
            var m = currentMonth - offset
            var y = currentYear
            while (m <= 0) {
                m += 12
                y -= 1
            }
            val amt = walletFiltered.filter { tx ->
                tx.type == TransactionType.EXPENSE &&
                    tx.monthIndex == m && tx.year == y &&
                    tx.label.split(",").any { it.trim().equals(labelName, ignoreCase = true) }
            }.sumOf { it.amount }
            Triple(shortMonthLabels.getOrElse(m) { "" }, amt, m == currentMonth && y == currentYear)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_back_label_detail")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextWhite)
                    }
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = AccentBlueBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = labelName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated
                    ) {
                        Text(
                            text = selectedWallet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthOptions, key = { it.year * 100 + it.monthIndex }) { opt ->
                        val isSel = opt.monthIndex == currentMonth && opt.year == currentYear
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) AccentBlue else DarkSurfaceElevated,
                            modifier = Modifier.clickable {
                                currentMonth = opt.monthIndex
                                currentYear = opt.year
                            }
                        ) {
                            Text(
                                text = opt.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Pengeluaran Label", fontSize = 11.5.sp, color = TextMuted)
                                        Text(
                                            text = CurrencyHelper.formatRupiah(expenseTotal),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                        if (incomeTotal > 0.0) {
                                            Text(
                                                text = "Pemasukan: +${CurrencyHelper.formatRupiah(incomeTotal)}",
                                                fontSize = 11.5.sp,
                                                color = IncomeGreen
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DarkSurfaceElevated,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showBarChart = !showBarChart }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (showBarChart) Icons.Default.ShowChart else Icons.Default.BarChart,
                                                contentDescription = null,
                                                tint = AccentBlueBright,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (showBarChart) "Garis" else "Batang",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AccentBlueBright
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                SixMonthMiniChart(
                                    data = sixMonthsTrend,
                                    accentColor = AccentBlueBright,
                                    isBar = showBarChart
                                )
                            }
                        }
                    }

                    item {
                        DetailTransactionsCard(
                            title = "Transaksi Berlabel #$labelName (${matchingTxs.size})",
                            transactions = matchingTxs,
                            onTransactionClick = onTransactionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SixMonthMiniChart(
    data: List<Triple<String, Double, Boolean>>,
    accentColor: Color,
    isBar: Boolean
) {
    val maxVal = (data.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (isBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (label, value, isCurrent) ->
                    val ratio = (value / maxVal).toFloat().coerceIn(0.05f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (value > 0.0) {
                            Text(
                                text = CurrencyHelper.formatCompact(value),
                                fontSize = 9.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) accentColor else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((72 * ratio).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (value <= 0.0) DarkSurfaceElevated
                                    else if (isCurrent) accentColor
                                    else accentColor.copy(alpha = 0.4f)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) TextWhite else TextMuted
                        )
                    }
                }
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                if (data.size >= 2) {
                    val path = Path()
                    val stepX = size.width / (data.size - 1).coerceAtLeast(1)
                    val pts = data.mapIndexed { idx, (_, v, _) ->
                        val ratio = (v / maxVal).toFloat().coerceIn(0f, 1f)
                        Offset(idx * stepX, size.height - (ratio * size.height * 0.85f) - 4.dp.toPx())
                    }
                    pts.forEachIndexed { idx, p ->
                        if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    pts.forEachIndexed { idx, p ->
                        val isCur = data[idx].third
                        drawCircle(
                            color = if (isCur) accentColor else DarkSurface,
                            radius = if (isCur) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = p
                        )
                        if (!isCur) {
                            drawCircle(
                                color = accentColor,
                                radius = 3.5.dp.toPx(),
                                center = p,
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { (label, _, isCur) ->
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCur) TextWhite else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTransactionsCard(
    title: String,
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "Belum ada transaksi pada periode ini.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val grouped = transactions.groupBy { "${it.dayOfWeekName}, ${it.dayOfMonth} ${shortMonthLabels.getOrElse(it.monthIndex) { "" }}" }
                grouped.entries.forEachIndexed { gIdx, (dateHeader, dayTxs) ->
                    if (gIdx > 0) Spacer(modifier = Modifier.height(12.dp))
                    val dayTotal = dayTxs.sumOf {
                        when (it.type) {
                            TransactionType.INCOME -> it.amount
                            TransactionType.EXPENSE -> -it.amount
                            TransactionType.TRANSFER -> 0.0
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateHeader, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        Text(
                            text = (if (dayTotal > 0) "+" else "") + CurrencyHelper.formatCompactRupiah(dayTotal),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dayTotal >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    dayTxs.forEachIndexed { tIdx, tx ->
                        if (tIdx > 0) HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.28f), modifier = Modifier.padding(vertical = 6.dp))
                        val catColor = CategoryIconHelper.getCategoryColor(tx.category, tx.type)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTransactionClick(tx) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(catColor.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getCategoryIcon(tx.category),
                                    contentDescription = tx.category,
                                    tint = catColor,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title.ifBlank { tx.category },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = listOfNotNull(
                                        tx.subcategory.takeIf { it.isNotBlank() },
                                        tx.accountName,
                                        tx.time.takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val prefix = when (tx.type) {
                                TransactionType.INCOME -> "+"
                                TransactionType.EXPENSE -> "-"
                                TransactionType.TRANSFER -> ""
                            }
                            val amtColor = when (tx.type) {
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.TRANSFER -> AccentBlueBright
                            }
                            Text(
                                text = "$prefix${CurrencyHelper.formatRupiah(tx.amount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = amtColor
                            )
                        }
                    }
                }
            }
        }
    }
}
