package data.util

import androidx.compose.ui.geometry.Offset
import data.Point2D
import java.io.File
import kotlin.math.*

object PointOperations {

    fun interpolatePoints(start: Offset, end: Offset): List<Offset> {
        val distance = distanceTo(start, end)
        val numberOfPoints = max(1, distance.roundToInt())
        return List(numberOfPoints) { step ->
            Offset(
                lerp(start.x, end.x, step.toFloat() / numberOfPoints),
                lerp(start.y, end.y, step.toFloat() / numberOfPoints)
            )
        }
    }

    fun distanceTo(start: Offset, end: Offset) = sqrt((end.x - start.x).pow(2) + (end.y - start.y).pow(2))

    fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

    fun adjustPoints(points: List<Offset>): List<Point2D> {
        val (cx, cy) = points.map { it.x }.average() to points.map { it.y }.average()
        val centered = points.map { Point2D(x = it.x - cx, y = it.y - cy) }

        val mFactor = max(centered.maxOf { abs(it.x) }, centered.maxOf { abs(it.y) })
        val scaled = centered.map { Point2D(it.x / mFactor, it.y / mFactor) }

        return scaled
    }

    fun saveCoordinates(sign: String, points: List<List<Offset>>) {
        val file = File("$sign-coordinates.txt")

        file.bufferedWriter().use { out ->
            points.forEachIndexed { index, character ->
                val adjustedPoints = adjustPoints(character.distinct())
                adjustedPoints.forEach { point ->
                    out.write("${point.x}, ${point.y}\n")
                }

                if (index != points.lastIndex) {
                    out.write("NEXT\n")
                }
            }
        }
    }
}