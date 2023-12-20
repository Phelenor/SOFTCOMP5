package neural

import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.sum
import org.jetbrains.kotlinx.multik.ndarray.operations.times

object MeanSquaredError {

    fun compute(predictions: Matrix, targets: Matrix): Double {
        val difference = predictions - targets
        val squaredDifference = difference * difference
        val mse = squaredDifference.sum() / (difference.shape[0] * difference.shape[1])
        return mse
    }
}