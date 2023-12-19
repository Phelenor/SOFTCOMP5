package util

import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D1Array
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import kotlin.math.exp
import kotlin.math.max

fun D2Array<Double>.broadcastAdd(matrix: D2Array<Double>): D2Array<Double> {
    for (i in 0 until shape[0]) {
        this[i] += matrix.reshape(matrix.shape[0])
    }

    return this
}

fun D2Array<Double>.sigmoid(): D2Array<Double> {
    for (i in 0 until shape[0]) {
        for (j in 0 until shape[1]) {
            this[i, j] = sigmoid(this[i, j])
        }
    }

    return this
}

fun sigmoid(x: Double) = 1.0 / (1.0 + exp(-x))