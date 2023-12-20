package data

data class Config(
    val layers: MutableList<Int>,
    val learningRate: Double,
    val epsilon: Double,
    val epochs: Int,
    val mode: String,
    val representativePoints: Int
)