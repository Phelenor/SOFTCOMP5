package neural

import com.google.gson.annotations.SerializedName
import neural.NeuralNetwork.Mode.*
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import org.jetbrains.kotlinx.multik.ndarray.operations.toListD2

typealias Matrix = D2Array<Double>

class NeuralNetwork(private val layers: List<Int>) {

    private val gradients = mutableListOf<Matrix>()
    private val outputs = mutableListOf<Matrix>()
    private val weights: MutableList<Matrix> = List<NDArray<Double, D2>>(layers.lastIndex) { i ->
        mk.rand(-0.1, 0.1, intArrayOf(layers[i + 1], layers[i]))
    }.toMutableList()

    fun train(X: Matrix, y: Matrix, mode: Mode = MINI_BATCH_GRADIENT_DESCENT, batchSize: Int = 10, epochs: Int, learningRate: Double, epsilon: Double) {
        for (epoch in 0 until epochs) {
            when (mode) {
                BATCH_GRADIENT_DESCENT -> {
                    train(X, y, learningRate)
                }

                STOCHASTIC_GRADIENT_DESCENT -> {
                    val indices = (0 until X.shape[1]).shuffled()
                    for (i in indices) {
                        val X_train = mk.ndarray(X[0 until X.shape[0], i..i].toListD2())
                        val y_train = mk.ndarray(y[0 until y.shape[0], i..i].toListD2())
                        train(X_train, y_train, learningRate)
                    }
                }

                MINI_BATCH_GRADIENT_DESCENT -> {
                    val indices = (0 until X.shape[1]).shuffled()
                    val batches = indices.chunked(batchSize)
                    for (batch in batches) {
                        val X_train = X.extractColumns(batch)
                        val y_train = y.extractColumns(batch)
                        train(X_train, y_train, learningRate)
                    }
                }
            }

            val predictions = predict(X)
            val mse = MeanSquaredError.compute(predictions, y)

            if (epoch % 100 == 0) {
                println("Epoch: $epoch/$epochs, MSE = $mse")
            }

            if (mse <= epsilon) {
                println("End - Epoch: $epoch/$epochs, MSE = $mse")
                break
            }
        }
    }

    private fun train(X: Matrix, y: Matrix, learningRate: Double) {
        forward(X)
        backward(X, y)
        optimize(learningRate)
    }

    private fun forward(X: Matrix) {
        outputs.clear()
        var net = X.copy()

        weights.forEach { weight ->
            net = SigmoidActivation.function(weight dot net)
            outputs.add(net)
        }
    }

    private fun backward(X: Matrix, y: Matrix) {
        gradients.clear()
        val errors = MutableList(weights.size) { mk.zeros<Double>(1, 1) }

        errors[errors.lastIndex] = (outputs.last() - y) * SigmoidActivation.derivative(outputs.last())

        for (i in weights.indices.reversed().drop(1)) {
            errors[i] = SigmoidActivation.derivative(outputs[i]) * (weights[i + 1].transpose() dot errors[i + 1])
        }

        gradients.add(errors[0] dot X.transpose())

        for (i in 1 until errors.size) {
            gradients.add(errors[i] dot outputs[i - 1].transpose())
        }
    }

    private fun optimize(learningRate: Double) {
        for (i in weights.indices) {
            weights[i] -= learningRate * gradients[i]
        }
    }

    fun predict(X: Matrix): Matrix {
        forward(X)
        return outputs.last()
    }

    enum class Mode {
        @SerializedName("BGD")
        BATCH_GRADIENT_DESCENT,

        @SerializedName("SGD")
        STOCHASTIC_GRADIENT_DESCENT,

        @SerializedName("MBGD")
        MINI_BATCH_GRADIENT_DESCENT;
    }
}

private fun Matrix.extractColumns(indices: List<Int>): Matrix {
    val columns = (0 until this.shape[0]).map { row ->
        indices.map { column ->
            this[row, column]
        }
    }

    return mk.ndarray(columns)
}