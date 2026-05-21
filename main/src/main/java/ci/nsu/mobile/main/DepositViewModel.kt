package ci.nsu.mobile.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ci.nsu.mobile.main.data.DepositCalculation
import ci.nsu.mobile.main.data.DepositRepository
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    STEP_ONE,
    STEP_TWO,
    RESULT,
    HISTORY,
    DETAILS
}

data class CalculationResult(
    val initialAmount: Double,
    val periodMonths: Int,
    val interestRate: Double,
    val monthlyTopUp: Double,
    val finalAmount: Double,
    val interestEarned: Double,
    val calculationDate: Long = System.currentTimeMillis()
) {
    fun toEntity(): DepositCalculation {
        return DepositCalculation(
            initialAmount = initialAmount,
            periodMonths = periodMonths,
            interestRate = interestRate,
            monthlyTopUp = monthlyTopUp,
            finalAmount = finalAmount,
            interestEarned = interestEarned,
            calculationDate = calculationDate
        )
    }
}

class DepositViewModel(private val repository: DepositRepository) : ViewModel() {
    val screen = MutableLiveData(AppScreen.HOME)
    val history = MutableLiveData<List<DepositCalculation>>(emptyList())
    val selectedCalculation = MutableLiveData<DepositCalculation?>(null)

    var initialAmountText = ""
    var periodMonthsText = ""
    var monthlyTopUpText = ""
    var selectedRate = 0.0
    var calculationResult: CalculationResult? = null

    fun openHome() {
        screen.value = AppScreen.HOME
    }

    fun openStepOne() {
        screen.value = AppScreen.STEP_ONE
    }

    fun openStepTwo(): String? {
        val validationError = validateStepOne()
        if (validationError != null) {
            return validationError
        }

        selectedRate = availableRates().first()
        screen.value = AppScreen.STEP_TWO
        return null
    }

    fun openHistory() {
        screen.value = AppScreen.HISTORY
        loadHistory()
    }

    fun openDetails(calculation: DepositCalculation) {
        selectedCalculation.value = calculation
        screen.value = AppScreen.DETAILS
    }

    fun availableRates(): List<Double> {
        val months = parsePeriod() ?: return emptyList()
        return when {
            months < 6 -> listOf(15.0)
            months < 12 -> listOf(10.0)
            else -> listOf(5.0)
        }
    }

    fun calculate(): String? {
        val stepOneError = validateStepOne()
        if (stepOneError != null) {
            return stepOneError
        }

        val initialAmount = parseAmount(initialAmountText) ?: return "Введите стартовый взнос."
        val months = parsePeriod() ?: return "Введите срок вклада."
        val monthlyTopUp = if (monthlyTopUpText.isBlank()) {
            0.0
        } else {
            parseAmount(monthlyTopUpText) ?: return "Введите корректное ежемесячное пополнение."
        }

        if (monthlyTopUp < 0) {
            return "Ежемесячное пополнение не может быть отрицательным."
        }

        val rate = selectedRate.takeIf { it > 0 } ?: availableRates().first()
        val monthlyRate = rate / 100.0 / 12.0
        var balance = initialAmount

        repeat(months) {
            balance += monthlyTopUp
            balance += balance * monthlyRate
        }

        val totalContributions = initialAmount + monthlyTopUp * months
        val earned = balance - totalContributions

        calculationResult = CalculationResult(
            initialAmount = initialAmount,
            periodMonths = months,
            interestRate = rate,
            monthlyTopUp = monthlyTopUp,
            finalAmount = balance,
            interestEarned = earned
        )
        screen.value = AppScreen.RESULT
        return null
    }

    fun saveCurrentResult() {
        val result = calculationResult ?: return
        viewModelScope.launch {
            repository.saveCalculation(result.toEntity())
            loadHistory()
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            history.value = repository.getHistory()
        }
    }

    private fun validateStepOne(): String? {
        val initialAmount = parseAmount(initialAmountText)
        val months = parsePeriod()

        if (initialAmount == null) {
            return "Введите корректный стартовый взнос."
        }
        if (initialAmount <= 0.0) {
            return "Стартовый взнос должен быть больше нуля."
        }
        if (months == null) {
            return "Введите корректный срок вклада в месяцах."
        }
        if (months <= 0) {
            return "Срок вклада должен быть больше нуля."
        }

        return null
    }

    private fun parseAmount(value: String): Double? {
        return value.trim().replace(',', '.').toDoubleOrNull()
    }

    private fun parsePeriod(): Int? {
        return periodMonthsText.trim().toIntOrNull()
    }
}

class DepositViewModelFactory(
    private val repository: DepositRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DepositViewModel::class.java)) {
            return DepositViewModel(repository) as T
        }
        throw IllegalArgumentException("Неизвестный класс ViewModel")
    }
}
