package ci.nsu.mobile.main.data

class DepositRepository(private val dao: DepositDao) {
    suspend fun saveCalculation(calculation: DepositCalculation): Long {
        return dao.insert(calculation)
    }

    suspend fun getHistory(): List<DepositCalculation> {
        return dao.getAll()
    }
}
