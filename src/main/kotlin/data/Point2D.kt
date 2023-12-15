package data

data class Point2D(var x: Double, var y: Double) {

    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())

    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
}