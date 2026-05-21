package ci.nsu.mobile.main

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import ci.nsu.mobile.main.data.AppDatabase
import ci.nsu.mobile.main.data.DepositCalculation
import ci.nsu.mobile.main.data.DepositRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: DepositViewModel
    private val russianLocale = Locale.forLanguageTag("ru-RU")
    private val moneyFormat = NumberFormat.getCurrencyInstance(russianLocale)
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", russianLocale)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DepositRepository(database.depositDao())
        viewModel = ViewModelProvider(
            this,
            DepositViewModelFactory(repository)
        )[DepositViewModel::class.java]

        viewModel.screen.observe(this) { screen ->
            when (screen) {
                AppScreen.HOME -> renderHome()
                AppScreen.STEP_ONE -> renderStepOne()
                AppScreen.STEP_TWO -> renderStepTwo()
                AppScreen.RESULT -> renderResult()
                AppScreen.HISTORY -> renderHistory()
                AppScreen.DETAILS -> renderDetails()
            }
        }

        viewModel.history.observe(this) {
            if (viewModel.screen.value == AppScreen.HISTORY) {
                renderHistory()
            }
        }
    }

    private fun renderHome() {
        val root = createScreen("Расчёт вкладов")

        root.addView(
            text(
                "Приложение рассчитывает итоговую сумму вклада, проценты и сохраняет историю расчётов.",
                size = 16f
            )
        )
        root.addView(space(16))
        root.addView(primaryButton("Рассчитать") { viewModel.openStepOne() })
        root.addView(primaryButton("История расчётов") { viewModel.openHistory() })
        root.addView(secondaryButton("Закрыть приложение") { finishAffinity() })
    }

    private fun renderStepOne() {
        val root = createScreen("Основные параметры")

        root.addView(text("Этап 1 из 2", size = 16f, bold = true))
        root.addView(text("Введите стартовую сумму и срок вклада.", size = 15f))
        root.addView(space(12))

        root.addView(
            editText(
                hint = "Стартовый взнос, ₽",
                value = viewModel.initialAmountText,
                decimal = true
            ) {
                viewModel.initialAmountText = it
            }
        )
        root.addView(
            editText(
                hint = "Срок вклада в месяцах",
                value = viewModel.periodMonthsText,
                decimal = false
            ) {
                viewModel.periodMonthsText = it
            }
        )

        root.addView(space(12))
        root.addView(buttonRow(
            secondaryButton("В начало") { viewModel.openHome() },
            primaryButton("Далее") {
                val error = viewModel.openStepTwo()
                if (error != null) {
                    showMessage(error)
                }
            }
        ))
    }

    private fun renderStepTwo() {
        val root = createScreen("Дополнительные параметры")
        val rates = viewModel.availableRates()

        root.addView(text("Этап 2 из 2", size = 16f, bold = true))
        root.addView(text("Выберите ставку и укажите пополнение, если оно есть.", size = 15f))
        root.addView(space(12))

        root.addView(label("Процентная ставка"))
        root.addView(rateSpinner(rates))
        root.addView(
            editText(
                hint = "Ежемесячное пополнение, ₽ (необязательно)",
                value = viewModel.monthlyTopUpText,
                decimal = true
            ) {
                viewModel.monthlyTopUpText = it
            }
        )

        root.addView(space(12))
        root.addView(buttonRow(
            secondaryButton("Назад") { viewModel.openStepOne() },
            primaryButton("Рассчитать") {
                val error = viewModel.calculate()
                if (error != null) {
                    showMessage(error)
                }
            }
        ))
    }

    private fun renderResult() {
        val result = viewModel.calculationResult
        val root = createScreen("Результат расчёта")

        if (result == null) {
            root.addView(text("Расчёт ещё не выполнен.", size = 16f))
            root.addView(primaryButton("В начало") { viewModel.openHome() })
            return
        }

        root.addView(resultCard(result))
        root.addView(space(12))

        val saveButton = primaryButton("Сохранить") {
            viewModel.saveCurrentResult()
            showMessage("Расчёт сохранён в историю.")
        }
        root.addView(saveButton)
        root.addView(secondaryButton("В начало") { viewModel.openHome() })
    }

    private fun renderHistory() {
        val root = createScreen("История расчётов")
        val history = viewModel.history.value.orEmpty()

        if (history.isEmpty()) {
            root.addView(text("История пока пуста. Выполните расчёт и сохраните результат.", size = 16f))
        } else {
            history.forEach { calculation ->
                root.addView(historyItem(calculation))
            }
        }

        root.addView(space(12))
        root.addView(primaryButton("В начало") { viewModel.openHome() })
    }

    private fun renderDetails() {
        val calculation = viewModel.selectedCalculation.value
        val root = createScreen("Детали расчёта")

        if (calculation == null) {
            root.addView(text("Запись не выбрана.", size = 16f))
        } else {
            root.addView(resultCard(calculation))
        }

        root.addView(space(12))
        root.addView(buttonRow(
            secondaryButton("Назад к истории") { viewModel.openHistory() },
            primaryButton("В начало") { viewModel.openHome() }
        ))
    }

    private fun createScreen(title: String): LinearLayout {
        supportActionBar?.title = title

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(248, 249, 252))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scrollView.addView(root)
        setContentView(scrollView)

        root.addView(text(title, size = 26f, bold = true))
        root.addView(space(12))
        return root
    }

    private fun rateSpinner(rates: List<Double>): Spinner {
        val spinner = Spinner(this)
        val items = rates.map { "${formatRate(it)} годовых" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner.adapter = adapter
        spinner.layoutParams = fullWidthParams(top = 4, bottom = 12)
        val selectedIndex = rates.indexOf(viewModel.selectedRate).takeIf { it >= 0 } ?: 0
        if (rates.isNotEmpty()) {
            spinner.setSelection(selectedIndex)
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.selectedRate = rates[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        return spinner
    }

    private fun resultCard(result: CalculationResult): View {
        return card(
            listOf(
                "Дата расчёта: ${formatDate(result.calculationDate)}",
                "Стартовый взнос: ${formatMoney(result.initialAmount)}",
                "Срок вклада: ${result.periodMonths} мес.",
                "Процентная ставка: ${formatRate(result.interestRate)}",
                "Ежемесячное пополнение: ${formatMoney(result.monthlyTopUp)}",
                "Итоговая сумма: ${formatMoney(result.finalAmount)}",
                "Начисленные проценты: ${formatMoney(result.interestEarned)}"
            )
        )
    }

    private fun resultCard(calculation: DepositCalculation): View {
        return card(
            listOf(
                "Дата расчёта: ${formatDate(calculation.calculationDate)}",
                "Стартовый взнос: ${formatMoney(calculation.initialAmount)}",
                "Срок вклада: ${calculation.periodMonths} мес.",
                "Процентная ставка: ${formatRate(calculation.interestRate)}",
                "Ежемесячное пополнение: ${formatMoney(calculation.monthlyTopUp)}",
                "Итоговая сумма: ${formatMoney(calculation.finalAmount)}",
                "Начисленные проценты: ${formatMoney(calculation.interestEarned)}"
            )
        )
    }

    private fun historyItem(calculation: DepositCalculation): View {
        val card = card(
            listOf(
                "Дата: ${formatDate(calculation.calculationDate)}",
                "Стартовый взнос: ${formatMoney(calculation.initialAmount)}",
                "Итоговая сумма: ${formatMoney(calculation.finalAmount)}"
            )
        ) as LinearLayout
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener {
            viewModel.openDetails(calculation)
        }
        return card
    }

    private fun card(lines: List<String>): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(224, 226, 232))
            }
            layoutParams = fullWidthParams(bottom = 12)
        }

        lines.forEach { line ->
            container.addView(text(line, size = 15f))
        }
        return container
    }

    private fun editText(
        hint: String,
        value: String,
        decimal: Boolean,
        onChanged: (String) -> Unit
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            setSingleLine(true)
            inputType = if (decimal) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                InputType.TYPE_CLASS_NUMBER
            }
            layoutParams = fullWidthParams(bottom = 12)
            doAfterTextChanged { onChanged(it?.toString().orEmpty()) }
        }
    }

    private fun primaryButton(text: String, action: () -> Unit): Button {
        return button(text, Color.rgb(63, 81, 181), Color.WHITE, action)
    }

    private fun secondaryButton(text: String, action: () -> Unit): Button {
        return button(text, Color.WHITE, Color.rgb(63, 81, 181), action).apply {
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(63, 81, 181))
            }
        }
    }

    private fun button(text: String, backgroundColor: Int, textColor: Int, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextColor(textColor)
            textSize = 15f
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadius = dp(24).toFloat()
            }
            layoutParams = fullWidthParams(top = 6, bottom = 6)
            setOnClickListener { action() }
        }
    }

    private fun buttonRow(left: Button, right: Button): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = fullWidthParams()

            addView(left.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .withMargins(right = 6)
            })
            addView(right.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .withMargins(left = 6)
            })
        }
    }

    private fun text(value: String, size: Float, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.rgb(28, 31, 36))
            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
            setLineSpacing(0f, 1.15f)
            layoutParams = fullWidthParams(bottom = 8)
        }
    }

    private fun label(value: String): TextView {
        return text(value, size = 14f, bold = true)
    }

    private fun space(heightDp: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
            )
        }
    }

    private fun fullWidthParams(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).withMargins(top = top, bottom = bottom)
    }

    private fun LinearLayout.LayoutParams.withMargins(
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0
    ): LinearLayout.LayoutParams {
        setMargins(dp(left), dp(top), dp(right), dp(bottom))
        return this
    }

    private fun formatMoney(value: Double): String {
        return moneyFormat.format(value)
    }

    private fun formatRate(value: Double): String {
        return "${value.toInt()}%"
    }

    private fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
