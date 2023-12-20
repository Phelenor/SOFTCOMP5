package data

import neural.NeuralNetwork

data class Config(
    val layers: MutableList<Int>,
    val learningRate: Double,
    val epsilon: Double,
    val epochs: Int,
    val mode: NeuralNetwork.Mode,
    val representativePoints: Int
)