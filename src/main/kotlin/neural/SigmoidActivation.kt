package neural

import org.jetbrains.kotlinx.multik.api.math.exp
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ones
import org.jetbrains.kotlinx.multik.ndarray.operations.*

object SigmoidActivation {

    fun function(x: Matrix): Matrix {
        val one = mk.ones<Double>(x.shape[0], x.shape[1])
        return one / (mk.math.exp(-x) + one)
    }

    fun derivative(x: Matrix): Matrix {
        val one = mk.ones<Double>(x.shape[0], x.shape[1])
        return x * (one - x)
    }
}

