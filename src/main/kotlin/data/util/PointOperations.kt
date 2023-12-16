package data.util

import androidx.compose.ui.geometry.Offset
import data.LabeledData
import data.Point2D
import signs
import java.io.File
import kotlin.math.*

object PointOperations {

    fun loadData(): List<LabeledData> {
        val samples = signs.map { sign ->
            parseGestureFile("$sign-coordinates.txt")
        }

        val data = samples.mapIndexed { index, gesturesForSign ->
            val representativePoints = gesturesForSign.map { gesture -> getRepresentativePoints(20, gesture) }
            LabeledData(representativePoints, index)
        }

        return data
    }

    fun parseGestureFile(path: String): List<List<Point2D>> {
        val gestures = mutableListOf<List<Point2D>>()
        val content = File(path).readText()
        val gestureStrings = content.trim().split("NEXT\n")

        for (gestureString in gestureStrings) {
            val points = gestureString.trim().split("\n")
                .filter { it.isNotEmpty() }
                .map { line ->
                    val (x, y) = line.split(", ").map { it.toDouble() }
                    Point2D(x, y)
                }
            gestures.add(points)
        }

        return gestures
    }

    fun getRepresentativePoints(representativeCount: Int, points: List<Point2D>): List<Point2D> {
        val length = points.zipWithNext { a, b -> distanceTo(a, b) }.sum()
        val interval = length / (representativeCount - 1)
        val representativePoints = mutableListOf(points.first())

        var currentDistance = 0.0
        var nextDistanceThreshold = 0.0

        for (i in 0 until points.lastIndex) {
            val current = points[i]
            val next = points[i + 1]
            val segmentLength = distanceTo(current, next)
            currentDistance += segmentLength

            while (currentDistance >= nextDistanceThreshold && representativePoints.size < representativeCount) {
                representativePoints.add(next)
                nextDistanceThreshold += interval
            }
        }

        if (representativePoints.last() != points.last()) {
            representativePoints.add(points.last())
        }

        return representativePoints.distinct()
    }

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
    fun distanceTo(start: Point2D, end: Point2D) = sqrt((end.x - start.x).pow(2) + (end.y - start.y).pow(2))

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