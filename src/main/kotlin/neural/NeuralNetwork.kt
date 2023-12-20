package neural

import neural.NeuralNetwork.Mode.*
import org.jetbrains.kotlinx.multik.api.linalg.dot
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.rand
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D2
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.times

typealias Matrix = D2Array<Double>

class NeuralNetwork(private val layers: List<Int>) {

    private val gradients = mutableListOf<Matrix>()
    private val activationOutputs = mutableListOf<Matrix>()
    private val weights: MutableList<Matrix> = List<NDArray<Double, D2>>(layers.lastIndex) { i ->
        mk.rand(-0.1, 0.1, intArrayOf(layers[i + 1], layers[i]))
    }.toMutableList()

    fun train(X: Matrix, y: Matrix, mode: Mode = MINI_BATCH_BACKPROPAGATION, epochs: Int, learningRate: Double, epsilon: Double) {
        for (epoch in 0 until epochs) {
            when (mode) {
                BACKPROPAGATION -> train(X, y, learningRate)
                STOCHASTIC_BACKPROPAGATION -> train(X, y, learningRate)
                MINI_BATCH_BACKPROPAGATION -> train(X, y, learningRate)
            }

            val predictions = predict(X)
            val mse = MeanSquaredError.compute(predictions, y)

            if (epoch % 1000 == 0) {
                println("Epoch: $epoch/$epochs, MSE = $mse")
            }

            if (mse < epsilon) {
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
        activationOutputs.clear()
        var net = X.copy()

        weights.forEach { weight ->
            net = SigmoidActivation.function(weight dot net)
            activationOutputs.add(net)
        }
    }

    fun backward(X: Matrix, y: Matrix) {
        gradients.clear()
        val errors = MutableList(weights.size) { mk.zeros<Double>(1, 1) }

        errors[errors.lastIndex] = (activationOutputs.last() - y) * SigmoidActivation.derivative(activationOutputs.last())

        for (i in weights.indices.reversed().drop(1)) {
            errors[i] = SigmoidActivation.derivative(activationOutputs[i]) * (weights[i + 1].transpose() dot errors[i + 1])
        }

        gradients.add(errors[0] dot X.transpose())

        for (i in 1 until errors.size) {
            gradients.add(errors[i] dot activationOutputs[i - 1].transpose())
        }
    }

    private fun optimize(learningRate: Double) {
        for (i in weights.indices) {
            weights[i] -= learningRate * gradients[i]
        }
    }

    fun predict(X: Matrix): Matrix {
        forward(X)
        return activationOutputs.last()
    }

    enum class Mode {
        BACKPROPAGATION, STOCHASTIC_BACKPROPAGATION, MINI_BATCH_BACKPROPAGATION
    }
}