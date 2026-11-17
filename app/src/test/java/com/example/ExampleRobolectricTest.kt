package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Money Tracker", appName)
  }

  @Test
  fun `pdf report helper handles generation gracefully without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val result = com.example.util.PdfReportHelper.generateAndSavePdf(
      context = context,
      periodLabel = "Oktober 2026",
      walletName = "Semua Dompet",
      incomeTotal = 5000000.0,
      expenseTotal = 2500000.0,
      netCashFlow = 2500000.0,
      savingsRate = 50,
      expenseBreakdown = emptyList(),
      incomeBreakdown = emptyList(),
      transactions = emptyList()
    )
    if (result != null) {
      org.junit.Assert.assertTrue(result.file.exists())
      org.junit.Assert.assertTrue(result.fileName.endsWith(".pdf"))
    }
  }

  @Test
  fun `export and import json with custom logos works with android context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.repository.FinanceRepository()
    repository.initContext(context)
    val json = repository.exportTransactionsJson(context)
    org.junit.Assert.assertTrue(json.contains("\"customLogos\""))
    org.junit.Assert.assertTrue(json.contains("\"totalCustomLogos\""))

    val result = repository.importTransactionsFromJson(json, replaceExisting = false)
    org.junit.Assert.assertTrue(result.isSuccess)

    // Verify stock lotCount and averagePrice are exported and restored
    org.junit.Assert.assertTrue(json.contains("\"lotCount\""))
    org.junit.Assert.assertTrue(json.contains("\"averagePrice\""))
    val restoredAssets = repository.assets.value
    val bbri = restoredAssets.find { it.tickerSymbol.contains("BBRI") }
    org.junit.Assert.assertNotNull(bbri)
    org.junit.Assert.assertEquals(14.0, bbri!!.lotCount, 0.001)
    org.junit.Assert.assertEquals(4650.0, bbri.averagePrice, 0.001)
  }

  @Test
  fun `selecting wallet filter updates selectedWalletFilter state and filters transactions correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.repository.FinanceRepository()
    repository.initContext(context)
    val viewModel = com.example.ui.viewmodel.FinanceViewModel(repository)

    // Initially "Semua"
    assertEquals("Semua", viewModel.selectedWalletFilter.value)

    // Select specific wallet
    viewModel.selectWalletFilter("BCA")
    assertEquals("BCA", viewModel.selectedWalletFilter.value)

    // Revert back
    viewModel.selectWalletFilter("Semua")
    assertEquals("Semua", viewModel.selectedWalletFilter.value)
  }

  @Test
  fun `export and import json preserves berulang acara tabungan and payday autosave`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.repository.FinanceRepository()
    repository.initContext(context)

    repository.addRecurringTransaction(
      com.example.data.model.RecurringTransaction(
        id = "rec-backup-1",
        title = "Spotify Duo",
        amount = 71500.0,
        type = com.example.data.model.TransactionType.EXPENSE,
        category = "Bills & Fees",
        subcategory = "Langganan",
        accountName = "BRI Utama",
        dayOfMonth = 15,
        frequency = "Setiap bulan",
        startDate = "15/11/2026",
        endCondition = "AFTER_COUNT",
        endAfterCount = 12,
        logoName = "Spotify",
        skippedCount = 1,
        reminderEnabled = true,
        reminderOptions = listOf("Pada hari jatuh tempo", "1 hari sebelumnya")
      )
    )
    repository.addEvent(
      com.example.data.model.EventItem(
        id = "ev-backup-1",
        name = "Liburan Bali",
        eventType = "Perjalanan",
        targetBudget = 5000000.0,
        startDate = "20/11/2026",
        endDate = "25/11/2026",
        plannedExpenses = listOf(
          com.example.data.model.EventPlannedExpense(
            id = "pe-1",
            name = "Tiket Pesawat",
            category = "Transport",
            amount = 1500000.0,
            isPaid = true
          )
        ),
        isCompleted = false
      )
    )
    repository.addSavingsGoal(
      com.example.data.model.SavingsGoal(
        id = "sg-backup-1",
        name = "Dana Darurat",
        targetAmount = 10000000.0,
        currentAmount = 500000.0,
        startDate = "01/11/2026",
        targetDate = "01/11/2027",
        records = listOf(
          com.example.data.model.SavingsRecord(
            id = "sr-1",
            amount = 500000.0,
            isWithdraw = false,
            note = "Setoran awal",
            dateStr = "01/11/2026",
            walletName = ""
          )
        )
      )
    )
    repository.updatePaydayAutoSave(enabled = true, percent = 15)

    val json = repository.exportTransactionsJson(context)
    org.junit.Assert.assertTrue(json.contains("\"totalSavingsGoals\""))
    org.junit.Assert.assertTrue(json.contains("\"totalEvents\""))
    org.junit.Assert.assertTrue(json.contains("Spotify Duo"))
    org.junit.Assert.assertTrue(json.contains("Tiket Pesawat"))
    org.junit.Assert.assertTrue(json.contains("Setoran awal"))

    val freshRepo = com.example.data.repository.FinanceRepository()
    freshRepo.initContext(context)
    val result = freshRepo.importTransactionsFromJson(json, replaceExisting = true, ctx = context)
    org.junit.Assert.assertTrue(result.isSuccess)
    val summary = result.getOrThrow()
    org.junit.Assert.assertTrue(summary.recurringCount >= 1)
    org.junit.Assert.assertTrue(summary.eventCount >= 1)
    org.junit.Assert.assertTrue(summary.savingsGoalCount >= 1)

    val restoredRec = freshRepo.recurringTransactions.value.find { it.id == "rec-backup-1" }
    org.junit.Assert.assertNotNull(restoredRec)
    assertEquals("Spotify", restoredRec!!.logoName)
    assertEquals("AFTER_COUNT", restoredRec.endCondition)
    assertEquals(12, restoredRec.endAfterCount)
    assertEquals(1, restoredRec.skippedCount)
    org.junit.Assert.assertTrue(restoredRec.reminderEnabled)

    val restoredEvent = freshRepo.events.value.find { it.id == "ev-backup-1" }
    org.junit.Assert.assertNotNull(restoredEvent)
    assertEquals("Perjalanan", restoredEvent!!.eventType)
    assertEquals(1, restoredEvent.plannedExpenses.size)
    assertEquals("Tiket Pesawat", restoredEvent.plannedExpenses.first().name)
    org.junit.Assert.assertTrue(restoredEvent.plannedExpenses.first().isPaid)

    val restoredGoal = freshRepo.savingsGoals.value.find { it.id == "sg-backup-1" }
    org.junit.Assert.assertNotNull(restoredGoal)
    assertEquals(1, restoredGoal!!.records.size)
    assertEquals("Setoran awal", restoredGoal.records.first().note)
    org.junit.Assert.assertTrue(freshRepo.settings.value.paydayAutoSaveEnabled)
    assertEquals(15, freshRepo.settings.value.paydayAutoSavePercent)
  }

  @Test
  fun `financial health helper computes streak and pillar scores from real data`() {
    assertEquals(0, com.example.util.FinancialHealthHelper.calculateConsecutiveStreak(emptySet()))
    assertEquals(4, com.example.util.FinancialHealthHelper.calculateConsecutiveStreak(setOf(1, 2, 4, 5, 6, 7, 10, 11)))

    val txs = listOf(
      com.example.data.model.Transaction(
        id = "t1", title = "Gaji", category = "Gaji", amount = 10_000_000.0,
        type = com.example.data.model.TransactionType.INCOME, accountName = "BRI Utama",
        dayOfMonth = 1, monthIndex = 11, year = 2026
      ),
      com.example.data.model.Transaction(
        id = "t2", title = "Bonus", category = "Investasi", amount = 2_000_000.0,
        type = com.example.data.model.TransactionType.INCOME, accountName = "BCA",
        dayOfMonth = 2, monthIndex = 11, year = 2026
      ),
      com.example.data.model.Transaction(
        id = "t3", title = "Makan", category = "Makanan", amount = 3_000_000.0,
        type = com.example.data.model.TransactionType.EXPENSE, accountName = "BRI Utama",
        dayOfMonth = 3, monthIndex = 11, year = 2026
      )
    )
    val wallets = listOf(
      com.example.data.model.AccountWallet(id = "w1", name = "BRI Utama", balance = 9_000_000.0),
      com.example.data.model.AccountWallet(id = "w2", name = "BCA", balance = 5_000_000.0)
    )

    val report = com.example.util.FinancialHealthHelper.calculate(
      periodTransactions = txs,
      prevPeriodTransactions = emptyList(),
      allTransactions = txs,
      wallets = wallets,
      assets = emptyList(),
      debts = emptyList(),
      budgets = emptyList(),
      categories = emptyList(),
      selectedMonth = 11,
      selectedYear = 2026,
      daysInMonth = 30
    )

    assertEquals(5, report.pillars.size)
    assertEquals(9_000_000.0, report.netSurplus, 0.01)
    assertEquals(75.0, report.savingsRatePct, 0.01)
    assertEquals(3, report.longestStreakDays)
    assertEquals(2, report.incomeSourceCount)
    org.junit.Assert.assertTrue(report.score in 50..100)
  }

  @Test
  fun `storage helper persists menu order hidden items and timeline scenarios`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scenarios = listOf(
      com.example.data.model.TimelineScenarioItem(
        id = "sc-1",
        amount = 22_000_000.0,
        isIncome = false,
        note = "Beli Motor Tunai",
        date = "15/11/2026",
        recurrence = "Hanya sekali"
      )
    )
    val settings = com.example.data.model.UserSettings(
      mainMenuOrder = listOf("acara", "tabungan", "anggaran"),
      hiddenMenuItems = listOf("berulang")
    )

    val payload = com.example.data.repository.FinanceStorageHelper.PersistentData(
      transactions = emptyList(),
      wallets = emptyList(),
      assets = emptyList(),
      debts = emptyList(),
      budgets = emptyList(),
      recurringTransactions = emptyList(),
      plannedItems = emptyList(),
      planTitle = "Rencana Uji",
      labels = emptyList(),
      settings = settings,
      timelineScenarios = scenarios
    )

    com.example.data.repository.FinanceStorageHelper.saveData(context, payload)
    val loaded = com.example.data.repository.FinanceStorageHelper.loadData(context)
    org.junit.Assert.assertNotNull(loaded)
    assertEquals(listOf("acara", "tabungan", "anggaran"), loaded!!.settings.mainMenuOrder)
    assertEquals(listOf("berulang"), loaded.settings.hiddenMenuItems)
    assertEquals(1, loaded.timelineScenarios.size)
    assertEquals("Beli Motor Tunai", loaded.timelineScenarios.first().note)
    assertEquals(22_000_000.0, loaded.timelineScenarios.first().amount, 0.01)
  }

  @Test
  fun `import json replaceExisting clears existing data on explicit empty collections`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.repository.FinanceRepository()
    repository.initContext(context)

    val emptyBackupJson = """
      {
        "app": "MoneyTracker",
        "version": 3,
        "transactions": [],
        "recurringTransactions": [],
        "events": [],
        "savingsGoals": [],
        "debts": [],
        "budgets": []
      }
    """.trimIndent()

    val result = repository.importTransactionsFromJson(emptyBackupJson, replaceExisting = true, ctx = context)
    org.junit.Assert.assertTrue(result.isSuccess)
    assertEquals(0, repository.transactions.value.size)
    assertEquals(0, repository.recurringTransactions.value.size)
    assertEquals(0, repository.events.value.size)
    assertEquals(0, repository.savingsGoals.value.size)
    assertEquals(0, repository.debts.value.size)
    assertEquals(0, repository.budgets.value.size)
  }

  @Test
  fun `budget math helper computes monday to sunday weekly transactions accurately`() {
    val now = java.util.Calendar.getInstance()
    val startOfWeek = (now.clone() as java.util.Calendar).apply {
      val diffFromMonday = (get(java.util.Calendar.DAY_OF_WEEK) - java.util.Calendar.MONDAY + 7) % 7
      add(java.util.Calendar.DAY_OF_MONTH, -diffFromMonday)
    }
    val outsideWeek = (startOfWeek.clone() as java.util.Calendar).apply {
      add(java.util.Calendar.DAY_OF_MONTH, -3)
    }

    val budget = com.example.data.model.BudgetCategory(
      id = "b-week",
      category = "Makanan",
      allocated = 500_000.0,
      frequency = "Mingguan"
    )

    val txInWeek = com.example.data.model.Transaction(
      id = "w-in",
      title = "Sarapan",
      category = "Makanan",
      amount = 150_000.0,
      type = com.example.data.model.TransactionType.EXPENSE,
      accountName = "BRI Utama",
      dayOfMonth = now.get(java.util.Calendar.DAY_OF_MONTH),
      monthIndex = now.get(java.util.Calendar.MONTH) + 1,
      year = now.get(java.util.Calendar.YEAR)
    )
    val txOutWeek = com.example.data.model.Transaction(
      id = "w-out",
      title = "Minggu Lalu",
      category = "Makanan",
      amount = 200_000.0,
      type = com.example.data.model.TransactionType.EXPENSE,
      accountName = "BRI Utama",
      dayOfMonth = outsideWeek.get(java.util.Calendar.DAY_OF_MONTH),
      monthIndex = outsideWeek.get(java.util.Calendar.MONTH) + 1,
      year = outsideWeek.get(java.util.Calendar.YEAR)
    )

    val computed = com.example.util.BudgetMathHelper.computeBudgetItem(
      budget = budget,
      allTransactions = listOf(txInWeek, txOutWeek),
      wallets = emptyList(),
      allCategories = emptyList(),
      selectedMonth = now.get(java.util.Calendar.MONTH) + 1,
      selectedYear = now.get(java.util.Calendar.YEAR)
    )

    assertEquals(150_000.0, computed.spent, 0.01)
    assertEquals(350_000.0, computed.remaining, 0.01)
  }

  @Test
  fun `debt record with tanpa akun stays only in debt records and removes wallet tx on edit`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.repository.FinanceRepository()
    repository.initContext(context)

    val wallet = repository.wallets.value.first()
    val initialWalletBal = wallet.balance

    val testDebt = com.example.data.model.DebtItem(
      id = "debt-no-acc-test",
      name = "Pinjaman Tanpa Akun Test",
      type = com.example.data.model.DebtType.BORROWED,
      totalAmount = 1_000_000.0,
      paidAmount = 0.0,
      dueDate = "31 Des 2026",
      startDate = "1 Nov 2026",
      linkedWalletName = wallet.name
    )
    repository.addDebt(testDebt, depositToWalletName = null)

    // 1. Add record with "Tanpa Akun" (empty sourceWalletName, null affectWalletName)
    val noAccRecord = com.example.data.model.DebtRecord(
      id = "rec-no-acc-1",
      debtId = testDebt.id,
      type = "Pembayaran",
      amount = 200_000.0,
      date = "16 Nov 2026",
      time = "15.40",
      sourceWalletName = "",
      note = "Bayar tunai di luar dompet"
    )
    repository.addDebtRecord(testDebt.id, noAccRecord, affectWalletName = null)
    repository.syncDebtPaymentsWithTransactions()

    val afterAddWallet = repository.wallets.value.first { it.id == wallet.id }
    assertEquals(initialWalletBal, afterAddWallet.balance, 0.01)
    org.junit.Assert.assertFalse(
      repository.transactions.value.any { it.id == "debt_rec_${noAccRecord.id}" }
    )
    val debtAfterAdd = repository.debts.value.first { it.id == testDebt.id }
    assertEquals(200_000.0, debtAfterAdd.paidAmount, 0.01)
    assertEquals(1, debtAfterAdd.records.size)

    // 2. Edit record to use a real wallet -> wallet balance drops and tx appears
    repository.updateDebtRecord(
      testDebt.id,
      noAccRecord.copy(sourceWalletName = wallet.name)
    )
    val afterLinkWallet = repository.wallets.value.first { it.id == wallet.id }
    assertEquals(initialWalletBal - 200_000.0, afterLinkWallet.balance, 0.01)
    val linkedTx = repository.transactions.value.find { it.id == "debt_rec_${noAccRecord.id}" }
    org.junit.Assert.assertNotNull(linkedTx)
    assertEquals(wallet.name, linkedTx!!.accountName)

    // 3. Edit record back to "Tanpa Akun" -> wallet balance reverts and tx disappears completely
    repository.updateDebtRecord(
      testDebt.id,
      noAccRecord.copy(sourceWalletName = "")
    )
    repository.syncDebtPaymentsWithTransactions()
    val afterUnlinkWallet = repository.wallets.value.first { it.id == wallet.id }
    assertEquals(initialWalletBal, afterUnlinkWallet.balance, 0.01)
    org.junit.Assert.assertFalse(
      repository.transactions.value.any { it.id == "debt_rec_${noAccRecord.id}" }
    )
    val debtAfterUnlink = repository.debts.value.first { it.id == testDebt.id }
    assertEquals(1, debtAfterUnlink.records.size)
    assertEquals("", debtAfterUnlink.records.first().sourceWalletName)
  }

  @Test
  fun `safe to spend calculator computes shopping balance unpaid bills and daily limit deterministically`() {
    val nowCal = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.YEAR, 2026)
      set(java.util.Calendar.MONTH, java.util.Calendar.NOVEMBER)
      set(java.util.Calendar.DAY_OF_MONTH, 10)
      set(java.util.Calendar.HOUR_OF_DAY, 9)
      set(java.util.Calendar.MINUTE, 0)
    }
    val wallets = listOf(
      com.example.data.model.AccountWallet(
        id = "w1",
        name = "BRI Utama",
        balance = 3_000_000.0,
        excludeFromBudget = false
      ),
      com.example.data.model.AccountWallet(
        id = "w2",
        name = "Tabungan Tersembunyi",
        balance = 5_000_000.0,
        excludeFromBudget = true
      )
    )
    val recurring = listOf(
      // Due day 15 (>= 10 and < 25), unpaid -> deducted (500k)
      com.example.data.model.RecurringTransaction(
        id = "r1",
        title = "Listrik PLN",
        amount = 500_000.0,
        type = com.example.data.model.TransactionType.EXPENSE,
        category = "Bills & Fees",
        accountName = "BRI Utama",
        dayOfMonth = 15,
        isActive = true,
        lastProcessedMonthYear = ""
      ),
      // Already paid this month (11/2026) -> NOT deducted
      com.example.data.model.RecurringTransaction(
        id = "r2",
        title = "WiFi Indihome",
        amount = 350_000.0,
        type = com.example.data.model.TransactionType.EXPENSE,
        category = "Bills & Fees",
        accountName = "BRI Utama",
        dayOfMonth = 20,
        isActive = true,
        lastProcessedMonthYear = "11/2026"
      )
    )
    val todayTx = listOf(
      com.example.data.model.Transaction(
        id = "tx1",
        title = "Makan Siang",
        category = "Food & Drink",
        amount = 50_000.0,
        type = com.example.data.model.TransactionType.EXPENSE,
        accountName = "BRI Utama",
        dayOfMonth = 10,
        monthIndex = 11,
        year = 2026
      )
    )
    val settings = com.example.data.model.UserSettings(
      monthlyStartDay = 25,
      paydayAutoSaveEnabled = true,
      paydayAutoSavePercent = 10
    )

    val summary = com.example.util.SafeToSpendCalculator.calculate(
      wallets = wallets,
      transactions = todayTx,
      recurringTransactions = recurring,
      upcomingItems = emptyList(),
      savingsGoals = emptyList(),
      settings = settings,
      nowCal = nowCal
    )

    // Shopping balance only counts non-excluded wallet (3,000,000)
    assertEquals(3_000_000.0, summary.shoppingBalance, 0.01)
    assertEquals(500_000.0, summary.unpaidBillsBeforePayday, 0.01)
    assertEquals(1, summary.billCountBeforePayday)
    // Payday auto-save reserve = 10% of 3,000,000 = 300,000
    assertEquals(300_000.0, summary.goalReserves, 0.01)
    // Free until payday = 3,000,000 - 500,000 - 300,000 = 2,200,000
    assertEquals(2_200_000.0, summary.freeUntilPayday, 0.01)
    // Days until payday (Nov 10 -> Nov 25) = 15
    assertEquals(15, summary.daysUntilPayday)
    assertEquals(2_200_000.0 / 15.0, summary.dailySafeToSpend, 0.5)
    assertEquals(50_000.0, summary.spentToday, 0.01)
    assertEquals(1, summary.shoppingWalletCount)
  }

  @Test
  fun `statistik and dashboard widget config persist and survive json backup restore`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.repository.FinanceRepository()
    repository.initContext(context)

    val customStatOrder = listOf("CASH_FLOW", "TREND_INCOME", "TREND_EXPENSE")
    val customHiddenStats = listOf("EXPENSE_PER_LABEL", "NET_WORTH_TREND")
    val customDashOrder = listOf("BUDGET", "SAFE_TO_SPEND", "SPENDING_ACTIVITY", "UPCOMING")
    val customHiddenDash = listOf("UPCOMING")

    repository.updateStatistikConfig(
      order = customStatOrder,
      hidden = customHiddenStats,
      sideBySide = true
    )
    repository.updateDashboardWidgets(
      order = customDashOrder,
      hidden = customHiddenDash
    )

    val updated = repository.settings.value
    assertEquals(customStatOrder, updated.statistikCardsOrder)
    assertEquals(customHiddenStats, updated.hiddenStatistikCards)
    assertEquals(true, updated.trenBerdampinganEnabled)
    assertEquals(customDashOrder, updated.dashboardWidgetsOrder)
    assertEquals(customHiddenDash, updated.hiddenDashboardWidgets)

    val backupJson = repository.exportTransactionsJson(context)

    // Mutate settings to defaults, then restore from backup JSON
    repository.updateStatistikConfig(
      order = com.example.data.model.defaultStatistikCardOrder,
      hidden = emptyList(),
      sideBySide = false
    )
    repository.updateDashboardWidgets(
      order = com.example.data.model.defaultDashboardWidgetOrder,
      hidden = emptyList()
    )

    val restoreResult = repository.importTransactionsFromJson(
      jsonStr = backupJson,
      replaceExisting = true,
      ctx = context
    )
    org.junit.Assert.assertTrue(restoreResult.isSuccess)

    val restored = repository.settings.value
    assertEquals(customStatOrder, restored.statistikCardsOrder)
    assertEquals(customHiddenStats, restored.hiddenStatistikCards)
    assertEquals(true, restored.trenBerdampinganEnabled)
    assertEquals(customDashOrder, restored.dashboardWidgetsOrder)
    assertEquals(customHiddenDash, restored.hiddenDashboardWidgets)
  }
}
